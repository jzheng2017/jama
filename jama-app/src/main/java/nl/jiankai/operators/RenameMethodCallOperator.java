package nl.jiankai.operators;

import nl.jiankai.api.ApiMapping;
import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Migration;
import nl.jiankai.api.Transformer;

import java.util.Map;

public class RenameMethodCallOperator<P> implements MigrationOperator {
    private final MethodCallTransformer<P> methodCallTransformer;
    private final Transformer<P> transformer;

    public RenameMethodCallOperator(MethodCallTransformer<P> methodCallTransformer, Transformer<P> transformer) {
        this.methodCallTransformer = methodCallTransformer;
        this.transformer = transformer;
    }

    @Override
    public void migrate(Migration migration) {
        ApiMapping start = migration.mapping();
        ApiMapping end = migration.end();

         transformer.addProcessor(methodCallTransformer.rename(start.original().signature(), end.target().name()));
    }
}
