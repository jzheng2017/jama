package nl.jiankai.operators;

import nl.jiankai.api.Migration;
import nl.jiankai.api.StatementTransformer;
import nl.jiankai.api.Transformer;

public class AttributeEncapsulationOperator<P> implements MigrationOperator {

    private final StatementTransformer<P> statementTransformer;
    private final Transformer<P> transformer;

    public AttributeEncapsulationOperator(StatementTransformer<P> statementTransformer, Transformer<P> transformer) {
        this.statementTransformer = statementTransformer;
        this.transformer = transformer;
    }

    @Override
    public void migrate(Migration migration) {
        String getterSignature = migration.mapping().context().get("getter").toString();
        transformer.addProcessor(statementTransformer.encapsulateAttribute(migration.mapping().original().signature(),getterSignature));
    }
}
