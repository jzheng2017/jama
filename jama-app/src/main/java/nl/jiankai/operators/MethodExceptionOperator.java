package nl.jiankai.operators;

import nl.jiankai.api.Migration;
import nl.jiankai.api.StatementTransformer;
import nl.jiankai.api.Transformer;

import java.util.stream.Collectors;

public class MethodExceptionOperator<P> implements MigrationOperator{
    private final StatementTransformer<P> statementTransformer;
    private final Transformer<P> transformer;
    public MethodExceptionOperator(StatementTransformer<P> statementTransformer, Transformer<P> transformer) {
        this.statementTransformer = statementTransformer;
        this.transformer = transformer;
    }

    @Override
    public void migrate(Migration migration) {
        String methodSignature = migration.end().target().signature();
        transformer.addProcessor(statementTransformer.handleCheckedException("org.apache.commons.text.WordUtils#capitalizeV3(String)", migration.getContext("exception").stream().map(String.class::cast).collect(Collectors.toSet())));
    }
}
