package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.operators.*;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonClassTransformer;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.FieldAccessTransformer;
import nl.jiankai.spoon.SpoonTransformer;
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static nl.jiankai.compiler.JDTCompilerProblemSolver.compile;
import static nl.jiankai.spoon.SpoonUtil.getLauncher;

public class Migrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private final File outputDirectory;

    public Migrator(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void migrate(GitRepository toMigrateProject, GitRepository dependencyProject, String startCommitId, String endCommitId, String newVersion) {
        long start = System.currentTimeMillis();
        setup(toMigrateProject);

        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
        LOGGER.info("Found {} refactorings", refactorings.size());

        Transformer<Processor<?>> transformer = new SpoonTransformer(toMigrateProject, outputDirectory);
        Launcher dependencyLauncher = getLauncher(dependencyProject);
        dependencyLauncher.buildModel();
        LOGGER.info("{} build sucessfully", dependencyProject.getId());

        var tracker = new ElementTransformationTracker();
        var methodCallTransformationProvider = new SpoonTransformationProvider<CtInvocation>();
        var fieldAccessTransformationProvider = new SpoonTransformationProvider<CtFieldAccess>();
        var classTransformationProvider = new SpoonTransformationProvider<CtClass>();
        var methodCallTransformer = new SpoonMethodCallTransformer(methodCallTransformationProvider);
        var statementTransformer = new FieldAccessTransformer(fieldAccessTransformationProvider);
        var classTransformer = new SpoonClassTransformer(classTransformationProvider);

        var migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();

        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migration, methodCallTransformationProvider, fieldAccessTransformationProvider, tracker, dependencyLauncher.getFactory()));
        transformer.addProcessor(methodCallTransformer.handle());
        transformer.addProcessor(statementTransformer.handle());
        transformer.addProcessor(classTransformer.handle());

        try {
            transformer.run();
            tracker.report();
        } catch (ModelBuildingException e) {
            LOGGER.warn("The project has errors", e);
        }

        long end = System.currentTimeMillis();
        LOGGER.info("It took {} seconds to migrate {}", (end - start)/1000, toMigrateProject.getId());
        compile(outputDirectory, dependencyProject, newVersion);
    }

    private void setup(GitRepository toMigrateProject) {
        try {
            FileUtils.deleteDirectory(outputDirectory);
            FileUtils.copyDirectory(toMigrateProject.getLocalPath(), outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void migrate(Migration migration, TransformationProvider<CtInvocation> methodCallTransformationProvider, TransformationProvider<CtFieldAccess> fieldAccessTransformationProvider, ElementTransformationTracker tracker, Factory dependencyFactory) {
        handleAttributeMigrations(migration, fieldAccessTransformationProvider, tracker);
        handleMethodMigrations(migration, methodCallTransformationProvider, tracker, dependencyFactory);
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
