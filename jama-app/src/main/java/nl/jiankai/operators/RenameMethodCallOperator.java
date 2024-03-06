package nl.jiankai.operators;

import nl.jiankai.api.ApiMapping;
import nl.jiankai.api.MethodCallTransformer;
import nl.jiankai.api.Migration;

import java.util.Map;

public class RenameMethodCallOperator implements MigrationOperator {
    private MethodCallTransformer methodCallTransformer;

    public RenameMethodCallOperator(MethodCallTransformer methodCallTransformer) {
        this.methodCallTransformer = methodCallTransformer;
    }

    @Override
    public void migrate(Migration migration) {
        ApiMapping start = migration.mapping();
        ApiMapping end = migration.end();

        methodCallTransformer.rename(start.original().signature(), end.target().name());
    }
}
