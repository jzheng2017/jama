package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.impl.CompositeProjectFactory;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.operators.*;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonStatementTransformer;
import nl.jiankai.spoon.SpoonTransformer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;

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
        try {
            FileUtils.deleteDirectory(outputDirectory);
            FileUtils.copyDirectory(toMigrateProject.getLocalPath(), outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
        LOGGER.info("Found {} refactorings", refactorings.size());
        Transformer<Processor<?>> transformer = new SpoonTransformer(toMigrateProject, outputDirectory);
        Launcher launcher = getLauncher(dependencyProject);
        launcher.buildModel();
        MethodCallTransformer<Processor<?>> methodCallTransformer = new SpoonMethodCallTransformer(launcher.getFactory());
        StatementTransformer<Processor<?>> statementTransformer = new SpoonStatementTransformer(launcher.getModel());
        MigrationPathEvaluator migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();
        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migration, transformer, statementTransformer, methodCallTransformer));
        try {
            transformer.run();
        } catch (ModelBuildingException e) {
            LOGGER.warn("The project has errors", e);
        }
        ProjectCoordinate coord = dependencyProject.getProjectVersion().coordinate();
        Project migratedProject = new CompositeProjectFactory().createProject(outputDirectory);
        Transformer<Processor<?>> compilationTransformer = new SpoonTransformer(migratedProject, outputDirectory);
        migratedProject.upgradeDependency(new Dependency(coord.groupId(), coord.artifactId(), newVersion));
        migratedProject.install();
        compile(migratedProject, compilationTransformer);
    }

    private void migrate(Migration migration, Transformer<Processor<?>> transformer, StatementTransformer<Processor<?>> statementTransformer, MethodCallTransformer<Processor<?>> methodCallTransformer) {
        handleAttributeMigrations(migration, transformer, statementTransformer);
        handleMethodMigrations(migration, transformer, methodCallTransformer, statementTransformer);
    }

    private static void handleAttributeMigrations(Migration migration, Transformer<Processor<?>> transformer, StatementTransformer<Processor<?>> statementTransformer) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();
        if (refactoringTypes.stream().anyMatch(RefactoringType::isAttributeRefactoring)) {
            var encapsulate = new AttributeEncapsulationOperator<>(statementTransformer, transformer);
            encapsulate.migrate(migration);
        }
    }

    private static void handleMethodMigrations(Migration migration, Transformer<Processor<?>> transformer, MethodCallTransformer<Processor<?>> methodCallTransformer, StatementTransformer<Processor<?>> statementTransformer) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodReferenceRefactoring)) {
            var referenceOperator = new ChangeMethodCallReferenceOperator<>(methodCallTransformer, transformer);
            referenceOperator.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodParameterRefactoring)) {
            var argumentsOperator = new MethodCallArgumentOperator<>(methodCallTransformer, transformer);
            argumentsOperator.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isExceptionRefactoring)) {
            var exception = new MethodExceptionOperator<>(statementTransformer, transformer);
            exception.migrate(migration);
        }
    }
}
