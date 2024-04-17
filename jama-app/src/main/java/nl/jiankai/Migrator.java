package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.impl.CompositeProjectFactory;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.operators.AttributeEncapsulationOperator;
import nl.jiankai.operators.RenameMethodCallOperator;
import nl.jiankai.operators.MethodCallArgumentOperator;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonStatementTransformer;
import nl.jiankai.spoon.SpoonTransformer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static nl.jiankai.compiler.JDTCompilerProblemSolver.compile;

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
        MigrationPathEvaluator migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();
        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migration, transformer));
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

    private void migrate(Migration migration, Transformer<Processor<?>> transformer) {
        MethodCallTransformer<Processor<?>> methodCallTransformer = new SpoonMethodCallTransformer();
        StatementTransformer<Processor<?>> statementTransformer = new SpoonStatementTransformer();
        Set<RefactoringType> refactoringTypes = migration.refactorings();
        handleAttributeMigrations(migration, transformer, refactoringTypes, statementTransformer);
        handleMethodMigrations(migration, transformer, refactoringTypes, methodCallTransformer);
    }

    private static void handleAttributeMigrations(Migration migration, Transformer<Processor<?>> transformer, Set<RefactoringType> refactoringTypes, StatementTransformer<Processor<?>> statementTransformer) {
        if (refactoringTypes.stream().anyMatch(RefactoringType::isAttributeRefactoring)) {
            var encapsulate = new AttributeEncapsulationOperator<>(statementTransformer, transformer);
            encapsulate.migrate(migration);
        }
    }

    private static void handleMethodMigrations(Migration migration, Transformer<Processor<?>> transformer, Set<RefactoringType> refactoringTypes, MethodCallTransformer<Processor<?>> methodCallTransformer) {
        if (refactoringTypes.contains(RefactoringType.METHOD_NAME)) {
            var rename = new RenameMethodCallOperator<>(methodCallTransformer, transformer);
            rename.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodParameterRefactoring)) {
            var argumentsOperator = new MethodCallArgumentOperator<>(methodCallTransformer, transformer);
            argumentsOperator.migrate(migration);
        }
    }
}
