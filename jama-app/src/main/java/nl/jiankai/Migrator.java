package nl.jiankai;

import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import nl.jiankai.api.*;
import nl.jiankai.migration.MigrationPathEvaluatorImpl;
import nl.jiankai.operators.MigrationOperator;
import nl.jiankai.operators.RenameMethodCallOperator;
import nl.jiankai.refactoringminer.RefactoringMinerImpl;
import nl.jiankai.spoon.SpoonMethodCallTransformer;
import nl.jiankai.spoon.SpoonMethodQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Migrator {
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
        MigrationPathEvaluator migrationPathEvaluator = new MigrationPathEvaluatorImpl();
        migrationPathEvaluator
                .evaluate(refactorings)
                .forEach(migration -> migrate(migratedProject, migration, methodCallTransformer));

        transformers.forEach(Transformer::run);
    }

    private void migrate(GitRepository migratedProject, Migration migration, MethodCallTransformer methodCallTransformer) {
        var rename = new RenameMethodCallOperator(methodCallTransformer);
        rename.migrate(migration);
    }

    private MigrationOperator operation(Migration migration) {
        return null;
    }
}
