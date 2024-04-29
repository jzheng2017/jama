package nl.jiankai.operators;

import nl.jiankai.api.*;

import java.util.Set;

public class ChangeMethodCallReferenceOperator<P> implements MigrationOperator {
    private MethodCallTransformer<P> methodCallTransformer;
    private Transformer<P> transformer;

    public ChangeMethodCallReferenceOperator(MethodCallTransformer<P> methodCallTransformer, Transformer<P> transformer) {
        this.methodCallTransformer = methodCallTransformer;
        this.transformer = transformer;
    }

    @Override
    public void migrate(Migration migration) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();

        if (refactoringTypes.contains(RefactoringType.METHOD_NAME)) {
            ApiMapping start = migration.mapping();
            ApiMapping end = migration.end();

            transformer.addProcessor(methodCallTransformer.rename(start.original().signature(), end.target().name()));
        }

        if (refactoringTypes.containsAll(Set.of(RefactoringType.MOVE_METHOD, RefactoringType.METHOD_NAME))) {
            ApiMapping start = migration.end();
            ApiMapping end = migration.end();

            transformer.addProcessor(methodCallTransformer.changeReference(start.original().signature(), end.target().signature()));
        } else if (refactoringTypes.contains(RefactoringType.MOVE_METHOD)){
            ApiMapping start = migration.end();
            ApiMapping end = migration.end();

            transformer.addProcessor(methodCallTransformer.changeReference(start.original().signature(), end.target().signature()));
        }
    }
}
