package nl.jiankai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.jiankai.api.*;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.TestReport;
import nl.jiankai.compiler.CompilationResult;
import nl.jiankai.impl.project.CompositeProjectFactory;
import nl.jiankai.impl.project.git.JGitRepositoryFactory;
import nl.jiankai.operators.*;
import nl.jiankai.spoon.*;
import nl.jiankai.spoon.transformations.SpoonTransformationProvider;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static nl.jiankai.compiler.JDTCompilerProblemSolver.compile;
import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class Migrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private final File outputDirectory;

    public Migrator(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @JsonIgnoreProperties({"id"})
    public record Statistics(String project, int totalAffectedClasses, long totalChanges, Set<String> affectedClasses,
                             List<TransformationCount> elementChanges,
                             List<CompilationResult> compilationResults,
                             TestReport testResults) implements Identifiable {
        @Override
        public String getId() {
            return project;
        }

        public record TransformationCount(String transformation, String element, long count) {
        }
    }

    public Statistics migrate(Project toMigrateProject, Project dependencyProject, Collection<Migration> migrations, Launcher dependencyLauncher, String newVersion) {
        long start = System.currentTimeMillis();
        setup(toMigrateProject, dependencyProject);

        Transformer<Processor<?>> transformer = new SpoonTransformer(toMigrateProject, outputDirectory);

        var tracker = new ElementTransformationTracker();

        collectClassMappings(migrations, tracker);
        var methodCallTransformationProvider = new SpoonTransformationProvider<CtInvocation>();
        var fieldAccessTransformationProvider = new SpoonTransformationProvider<CtFieldAccess>();
        var classTransformationProvider = new SpoonTransformationProvider<CtClass>();
        var referenceTransformationProvider = new SpoonTransformationProvider<CtTypeReference>();
        var methodCallTransformer = new SpoonMethodCallTransformer(methodCallTransformationProvider, tracker);
        var statementTransformer = new FieldAccessTransformer(fieldAccessTransformationProvider);
        var classTransformer = new SpoonClassTransformer(classTransformationProvider);
        var referenceTransformer = new SpoonReferenceTransformer(referenceTransformationProvider);

        migrations.forEach(migration -> migrate(migration, methodCallTransformationProvider, fieldAccessTransformationProvider, referenceTransformationProvider, tracker, dependencyLauncher.getFactory()));
        transformer.addProcessor(methodCallTransformer.handle());
        transformer.addProcessor(statementTransformer.handle());
        transformer.addProcessor(classTransformer.handle());
        transformer.addProcessor(referenceTransformer.handle()); //it's important that class references are migrated last as otherwise other code elements that reference these class references can not be matched anymore


        try {
            transformer.run();
            tracker.report();
        } catch (ModelBuildingException e) {
            LOGGER.warn("The project has errors", e);
        }

        Project migratedProject = new CompositeProjectFactory().createProject(outputDirectory);
        List<CompilationResult> compilationResults = compile(migratedProject, toMigrateProject, dependencyProject, newVersion, tracker);
        TestReport testReport = testAffectedClasses(tracker, migratedProject);

        long end = System.currentTimeMillis();
        LOGGER.info("It took {} seconds to migrate {}", (end - start) / 1000, toMigrateProject.getId());
        return new Statistics(
                migratedProject.getProjectVersion().toString(),
                tracker.affectedClasses().size(), tracker.changes(),
                tracker.affectedClasses(),
                tracker.elementChanges().entrySet().stream().map(event -> new Statistics.TransformationCount(event.getKey().transformation(), event.getKey().element(), event.getValue())).toList(),
                compilationResults,
                testReport);
    }

    private void collectClassMappings(Collection<Migration> migrations, ElementTransformationTracker tracker) {
        migrations.forEach(migration -> {
            Migration current = migration;

            while (current != null) {
                if (current.mapping().refactoringType().isClassReferenceRefactoring()) {
                    tracker.map(current.mapping().original().signature(), current.mapping().target().signature());
                }

                current = current.next();
            }
        });
    }

    private TestReport testAffectedClasses(ElementTransformationTracker tracker, Project migrationProject) {
        Set<String> affectedClasses = tracker.affectedClasses();
        LOGGER.info("{} classes affected", affectedClasses.size());
        if (!affectedClasses.isEmpty()) {
            Set<String> testClassesEquivalent = affectedClasses
                    .stream()
                    .map(classSignature -> classSignature + "Test") //we assume that the test classes are named according to ClassNameTest convention
                    .collect(Collectors.toSet());
            return migrationProject.test(testClassesEquivalent);
        }
        return new TestReport(0, 0, 0, 0, 0, true, "");
    }

    private void setup(Project toMigrateProject, Project dependencyProject) {
        try {
            FileUtils.deleteDirectory(outputDirectory);
            FileUtils.copyDirectory(toMigrateProject.getLocalPath(), outputDirectory);
            //delete caching from older version before migration
            FileUtils.deleteQuietly(new File(outputDirectory, "spoon.classpath.tmp"));
            FileUtils.deleteQuietly(new File(outputDirectory, "spoon.classpath-app.tmp"));
            FileUtils.deleteQuietly(new File(dependencyProject.getLocalPath(), "spoon.classpath.tmp"));
            FileUtils.deleteQuietly(new File(dependencyProject.getLocalPath(), "spoon.classpath-app.tmp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void migrate(Migration migration, TransformationProvider<CtInvocation> methodCallTransformationProvider, TransformationProvider<CtFieldAccess> fieldAccessTransformationProvider, TransformationProvider<CtTypeReference> constructorTransformationProvider, ElementTransformationTracker tracker, Factory dependencyFactory) {
        handleAttributeMigrations(migration, fieldAccessTransformationProvider, tracker);
        handleMethodMigrations(migration, methodCallTransformationProvider, tracker, dependencyFactory);
        handleClassMigrations(migration, constructorTransformationProvider, tracker, dependencyFactory);
    }

    private void handleClassMigrations(Migration migration, TransformationProvider<CtTypeReference> constructorTransformationProvider, ElementTransformationTracker tracker, Factory dependencyFactory) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();

        if (refactoringTypes.stream().anyMatch(RefactoringType::isClassReferenceRefactoring)) {
            var clazz = new ClassReferenceOperator(tracker, constructorTransformationProvider, dependencyFactory);
            clazz.migrate(migration);
        }
    }

    private static void handleAttributeMigrations(Migration migration, TransformationProvider<CtFieldAccess> fieldAccessTransformationProvider, ElementTransformationTracker tracker) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();
        if (refactoringTypes.stream().anyMatch(RefactoringType::isAttributeRefactoring)) {
            var encapsulate = new AttributeEncapsulationOperator(fieldAccessTransformationProvider, tracker);
            encapsulate.migrate(migration);
        }
    }

    private static void handleMethodMigrations(Migration migration, TransformationProvider<CtInvocation> transformationProvider, ElementTransformationTracker tracker, Factory dependencyFactory) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodParameterRefactoring)) {
            var argumentsOperator = new InvocationArgumentOperator(transformationProvider, tracker);
            argumentsOperator.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodReferenceRefactoring)) {
            var referenceOperator = new ChangeMethodCallReferenceOperator(tracker, transformationProvider, dependencyFactory);
            referenceOperator.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isExceptionRefactoring)) {
            var exception = new MethodExceptionOperator<>(transformationProvider, tracker, dependencyFactory.getModel());
            exception.migrate(migration);
        }
    }
}
