package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.migration.MigrationPathEvaluatorImpl;
import nl.jiankai.operators.MigrationOperator;
import nl.jiankai.operators.RenameMethodCallOperator;
import nl.jiankai.operators.ReorderArgumentMethodCallOperator;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonMethodQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl(new SpoonMethodQuery(dependencyProject));
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(dependencyProject, startCommitId, endCommitId);
        LOGGER.info("Found {} refactorings", refactorings.size());
        MigrationPathEvaluator migrationPathEvaluator = new MigrationPathEvaluatorImpl();
        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migratedProject, migration, methodCallTransformer));

        transformers.forEach(Transformer::run);
    }

    private void migrate(GitRepository migratedProject, Migration migration, MethodCallTransformer methodCallTransformer) {
        if (migration.mapping().refactoringType() == RefactoringType.METHOD_NAME) {
            var rename = new RenameMethodCallOperator(methodCallTransformer);
            rename.migrate(migration);
        } else if (migration.mapping().refactoringType() == RefactoringType.REORDER_PARAMETER) {
            var reorder = new ReorderArgumentMethodCallOperator(methodCallTransformer);
            reorder.migrate(migration);
        }
    }

    private MigrationOperator operation(Migration migration) {
        return null;
    }
}
