package nl.jiankai;

import nl.jiankai.api.*;
import nl.jiankai.migration.MigrationPathEvaluatorImpl;
import nl.jiankai.operators.MigrationOperator;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonMethodQuery;

import java.util.Collection;
import java.util.List;

public class Migrator {
    public void migrate(GitRepository gitRepository, String startCommitId, String endCommitId) {
        RefactoringMiner refactoringMiner = new RefactoringMinerImpl(new SpoonMethodQuery(gitRepository));
        Collection<Refactoring> refactorings = refactoringMiner.detectRefactoringBetweenCommit(gitRepository, startCommitId, endCommitId);
        MigrationPathEvaluator migrationPathEvaluator = new MigrationPathEvaluatorImpl();
        List<Migration> migrationList = migrationPathEvaluator.evaluate(refactorings).stream().toList();
    }

    public void migrate(Migration migration) {

    }

    private MigrationOperator operation(Migration migration) {
        return null;
    }
}
