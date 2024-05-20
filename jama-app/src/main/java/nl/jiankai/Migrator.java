package nl.jiankai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.jiankai.api.*;
import nl.jiankai.api.project.GitRepository;
import nl.jiankai.api.storage.CacheService;
import nl.jiankai.impl.JacksonSerializationService;
import nl.jiankai.impl.storage.MultiFileCacheService;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.operators.*;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
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
    private record RefactoringCollection(String startCommit, String endCommit,
                                         Collection<Refactoring> refactorings) implements Identifiable {
        @Override
        public String getId() {
            return startCommit + ":" + endCommit;
        }
    }

    public void migrate(GitRepository toMigrateProject, GitRepository dependencyProject, String startCommitId, String endCommitId, String newVersion) {
        long start = System.currentTimeMillis();
        setup(toMigrateProject);

        Collection<Refactoring> refactorings = getRefactorings(dependencyProject, startCommitId, endCommitId);

        LOGGER.info("Found {} refactorings", refactorings.size());

        Transformer<Processor<?>> transformer = new SpoonTransformer(toMigrateProject, outputDirectory);
        Launcher dependencyLauncher = getLauncher(dependencyProject);
        dependencyLauncher.buildModel();
        LOGGER.info("{} build sucessfully", dependencyProject.getId());

        var tracker = new ElementTransformationTracker();
        var methodCallTransformationProvider = new SpoonTransformationProvider<CtInvocation>();
        var fieldAccessTransformationProvider = new SpoonTransformationProvider<CtFieldAccess>();
        var classTransformationProvider = new SpoonTransformationProvider<CtClass>();
        var referenceTransformationProvider = new SpoonTransformationProvider<CtTypeReference>();
        var methodCallTransformer = new SpoonMethodCallTransformer(methodCallTransformationProvider);
        var statementTransformer = new FieldAccessTransformer(fieldAccessTransformationProvider);
        var classTransformer = new SpoonClassTransformer(classTransformationProvider);
        var referenceTransformer = new SpoonReferenceTransformer(referenceTransformationProvider);

        var migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();

        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migration, methodCallTransformationProvider, fieldAccessTransformationProvider, referenceTransformationProvider, tracker, dependencyLauncher.getFactory()));
        transformer.addProcessor(methodCallTransformer.handle());
        transformer.addProcessor(statementTransformer.handle());
        transformer.addProcessor(classTransformer.handle());
        transformer.addProcessor(referenceTransformer.handle());

        try {
            transformer.run();
            tracker.report();
        } catch (ModelBuildingException e) {
            LOGGER.warn("The project has errors", e);
        }

        long end = System.currentTimeMillis();
        compile(outputDirectory, dependencyProject, newVersion, tracker);
        testAffectedClasses(tracker, toMigrateProject);
        LOGGER.info("It took {} seconds to migrate {}", (end - start) / 1000, toMigrateProject.getId());
    }

    private void testAffectedClasses(ElementTransformationTracker tracker, GitRepository migrationProject) {
        Set<String> affectedClasses = tracker.affectedClasses();
        LOGGER.info("{} classes affected", affectedClasses.size());
        if (!affectedClasses.isEmpty()) {
            Set<String> testClassesEquivalent = affectedClasses
                    .stream()
                    .map(classSignature -> classSignature + "Test") //we assume that the test classes are named according to ClassNameTest convention
                    .collect(Collectors.toSet());
            migrationProject.test(testClassesEquivalent);
        }
    }

    private static Collection<Refactoring> getRefactorings(GitRepository dependencyProject, String startCommitId, String endCommitId) {
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        Collection<Refactoring> refactorings;
        CacheService<RefactoringCollection> cacheService = new MultiFileCacheService<>("/home/jiankai/test/cache", new JacksonSerializationService(), RefactoringCollection.class);

        if (cacheService.isCached(startCommitId + ":" + endCommitId)) {
            refactorings = cacheService
                    .get(startCommitId + ":" + endCommitId)
                    .orElseGet(() -> new RefactoringCollection(startCommitId, endCommitId, refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId)))
                    .refactorings();
        } else {
            refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
            RefactoringCollection refactoringCollection = new RefactoringCollection(startCommitId, endCommitId, refactorings);
            cacheService.write(refactoringCollection);
        }

        return refactorings;
    }

    private void setup(GitRepository toMigrateProject) {
        try {
            FileUtils.deleteDirectory(outputDirectory);
            FileUtils.copyDirectory(toMigrateProject.getLocalPath(), outputDirectory);
            //delete caching from older version before migration
            FileUtils.deleteQuietly(new File(outputDirectory, "spoon.classpath.tmp"));
            FileUtils.deleteQuietly(new File(outputDirectory, "spoon.classpath-app.tmp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void migrate(Migration migration, TransformationProvider<CtInvocation> methodCallTransformationProvider, TransformationProvider<CtFieldAccess> fieldAccessTransformationProvider, TransformationProvider<CtTypeReference> constructorTransformationProvider , ElementTransformationTracker tracker, Factory dependencyFactory) {
        handleAttributeMigrations(migration, fieldAccessTransformationProvider, tracker);
        handleMethodMigrations(migration, methodCallTransformationProvider, tracker, dependencyFactory);
        handleClassMigrations(migration, methodCallTransformationProvider, constructorTransformationProvider, tracker, dependencyFactory);
    }

    private void handleClassMigrations(Migration migration, TransformationProvider<CtInvocation> methodCallTransformationProvider, TransformationProvider<CtTypeReference> constructorTransformationProvider, ElementTransformationTracker tracker, Factory dependencyFactory) {
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
            var argumentsOperator = new MethodCallArgumentOperator(transformationProvider, tracker);
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
