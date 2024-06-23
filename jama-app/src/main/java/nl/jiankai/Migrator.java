package nl.jiankai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.jiankai.api.*;
import nl.jiankai.api.project.Dependency;
import nl.jiankai.api.project.Project;
import nl.jiankai.api.project.ProjectCoordinate;
import nl.jiankai.api.project.TestReport;
import nl.jiankai.compiler.CompilationResult;
import nl.jiankai.compiler.JDTCompilerProblemSolver;
import nl.jiankai.impl.project.CompositeProjectFactory;
import nl.jiankai.operators.*;
import nl.jiankai.spoon.*;
import nl.jiankai.spoon.transformations.SpoonTransformationProvider;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

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
    public record Statistics(String project, int totalAffectedClasses, long totalChanges, Set<String> affectedClasses, Set<String> untestedClasses,
                             List<TransformationCount> elementChanges,
                             CompilationResult compilationResultBeforeTransformation,
                             CompilationResult compilationResultAfterTransformation,
                             List<CompilationResult> compilationResultsDuringCompilationPhase,
                             TestReport testResults, String failureReason, long runTimeMs) implements Identifiable {
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
        String failureReason = "";
        Set<String> untestedClasses = Set.of();
        Project migratedProject = new CompositeProjectFactory().createProject(outputDirectory);
        boolean lowCoverage = false;
        CompilationResult resultBeforeTransformation = compileAndGetCompilationResult(migratedProject);
        CompilationResult resultAfterTransformation;
        try {
            transformer.run();
            resultAfterTransformation = compileAndGetCompilationResult(migratedProject);
            tracker.report();
            upgradeMigratedProject(dependencyProject, newVersion, migratedProject);
            untestedClasses = determineTestCoverage(tracker, migratedProject);
        } catch (ModelBuildingException e) {
            LOGGER.warn("The project has errors which is possible at this stage and an attempt will be done to fix it", e);
            failureReason = e.toString();
            resultAfterTransformation = compileAndGetCompilationResult(migratedProject);
            return failureStatistics(toMigrateProject, tracker, untestedClasses,"", failureReason, start, resultBeforeTransformation, resultAfterTransformation);
        } catch (LowTestCoverageException e) {
            failureReason = e.toString();
            lowCoverage = true;
            resultAfterTransformation = compileAndGetCompilationResult(migratedProject);
        } catch (Exception e) {
            failureReason = e.toString();
            resultAfterTransformation = compileAndGetCompilationResult(migratedProject);
            return failureStatistics(toMigrateProject, tracker, untestedClasses,"", failureReason, start, resultBeforeTransformation, resultAfterTransformation);
        }

        List<CompilationResult> compilationResults;
        TestReport testReport;

        try {
            compilationResults = compile(migratedProject, toMigrateProject, tracker);
        } catch (Exception e) {
            failureReason = e.toString();
            return failureStatistics(toMigrateProject, tracker, untestedClasses,"",failureReason, start, resultBeforeTransformation, resultAfterTransformation);
        }

        if (lowCoverage) {
            return failureStatistics(toMigrateProject, tracker, untestedClasses,"No tests were ran as there were too few tests",failureReason, start, resultBeforeTransformation, resultAfterTransformation);
        } else {
            try {
                testReport = testAffectedClasses(tracker, migratedProject);
            } catch (Exception e) {
                return failureStatistics(toMigrateProject, tracker, untestedClasses, e.toString(), e.toString(), start, resultBeforeTransformation, resultAfterTransformation);
            }


            long end = System.currentTimeMillis();
            LOGGER.info("It took {} seconds to migrate {}", (end - start) / 1000, toMigrateProject.getId());
            return new Statistics(
                    migratedProject.getProjectVersion().toString(),
                    tracker.affectedClasses().size(), tracker.changes(),
                    tracker.affectedClasses(),
                    untestedClasses,
                    tracker.elementChanges().entrySet().stream().map(event -> new Statistics.TransformationCount(event.getKey().transformation(), event.getKey().element(), event.getValue())).toList(),
                    resultBeforeTransformation,
                    resultAfterTransformation,
                    compilationResults,
                    testReport,
                    failureReason,
                    end - start
            );
        }
    }

    private static void upgradeMigratedProject(Project dependencyProject, String newVersion, Project migratedProject) {
        ProjectCoordinate coord = dependencyProject.getProjectVersion().coordinate();
        migratedProject.upgradeDependency(new Dependency(coord.groupId(), coord.artifactId(), newVersion));
        migratedProject.install();
    }

    private static Set<String> determineTestCoverage(ElementTransformationTracker tracker, Project migratedProject) {
        if (!tracker.affectedClasses().isEmpty()) {
            Set<Reference> classes = tracker.affectedClasses().stream().map(clazz -> new Reference(clazz, ReferenceType.CLASS)).collect(Collectors.toSet());
            Launcher migratedProjectLauncher = getLauncher(migratedProject);
            CtModel ctModel = migratedProjectLauncher.buildModel();
            Set<String> classesWithoutTest = new SpoonProjectQuality().getUntestedChanges(classes, ctModel.getRootPackage()).stream().map(Reference::fullyQualified).collect(Collectors.toSet());
            double ratioTested = (double) classesWithoutTest.size() / classes.size();
            if (ratioTested >= 0.5) {
                LOGGER.info("{} out of {} ({}%) classes are not covered by tests which is higher than the threshold (at least 50%)",classesWithoutTest.size(), classes.size(), ratioTested*100);
                throw new LowTestCoverageException(classesWithoutTest);
            }
            return classesWithoutTest;
        }
        return Set.of();
    }

    private static class LowTestCoverageException extends RuntimeException {
        private Set<String> untestedClasses;
        public LowTestCoverageException(Set<String> untestedClasses) {
            this.untestedClasses = untestedClasses;
        }

        public Set<String> getUntestedClasses() {
            return untestedClasses;
        }
    }

    private CompilationResult compileAndGetCompilationResult(Project project) {
        Launcher launcher = getLauncher(project);
        JDTBasedSpoonCompiler modelBuilder = (JDTBasedSpoonCompiler) launcher.getModelBuilder();
        try {
            modelBuilder.build();
            return JDTCompilerProblemSolver.createCompilationResult(0, modelBuilder);
        } catch (Exception e) {
            return JDTCompilerProblemSolver.createCompilationResult(0, modelBuilder);
        }
    }

    private static Statistics failureStatistics(Project toMigrateProject, ElementTransformationTracker tracker,
                                                Set<String> untestedClasses, String testFailureReason, String failureReason, long startTime,
                                                CompilationResult compilationResultBeforeTransformation, CompilationResult compilationResultAfterTransformation) {
        return new Statistics(toMigrateProject.getProjectVersion().toString(),
                tracker.affectedClasses().size(), tracker.changes(),
                tracker.affectedClasses(),
                untestedClasses,
                tracker.elementChanges().entrySet().stream().map(event -> new Statistics.TransformationCount(event.getKey().transformation(), event.getKey().element(), event.getValue())).toList(),
                compilationResultBeforeTransformation,
                compilationResultAfterTransformation,
                new ArrayList<>(),
                TestReport.failure(testFailureReason),
                failureReason,
                System.currentTimeMillis()-startTime
        );
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
            FileUtils.deleteDirectory(new File(outputDirectory.getAbsolutePath() + "_tmp"));
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
