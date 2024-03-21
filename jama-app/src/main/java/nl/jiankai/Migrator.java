package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.migration.MethodMigrationPathEvaluatorImpl;
import nl.jiankai.operators.RenameMethodCallOperator;
import nl.jiankai.operators.MethodCallArgumentOperator;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonMethodQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Migrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);
    private final File outputDirectory;
    private final List<Transformer> transformers = new ArrayList<>();

    public Migrator(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void migrate(GitRepository migratedProject, GitRepository dependencyProject, String startCommitId, String endCommitId) {
        MethodCallTransformer methodCallTransformer = new SpoonMethodCallTransformer(migratedProject, outputDirectory);
        transformers.add(methodCallTransformer);
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl();
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
        LOGGER.info("Found {} refactorings", refactorings.size());
        MigrationPathEvaluator migrationPathEvaluator = new MethodMigrationPathEvaluatorImpl();
        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migration, methodCallTransformer));

        transformers.forEach(Transformer::run);
    }

    private void migrate(Migration migration, MethodCallTransformer methodCallTransformer) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();
        if (refactoringTypes.contains(RefactoringType.METHOD_NAME)) {
            var rename = new RenameMethodCallOperator(methodCallTransformer);
            rename.migrate(migration);
        }

        if (refactoringTypes.stream().anyMatch(RefactoringType::isMethodParameterRefactoring)) {
            var argumentsOperator = new MethodCallArgumentOperator(methodCallTransformer);
            argumentsOperator.migrate(migration);
        }
    }
}
