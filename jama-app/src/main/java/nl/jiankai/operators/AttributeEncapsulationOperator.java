package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Migration;
import nl.jiankai.api.TransformationProvider;
import nl.jiankai.spoon.transformations.clazz.EncapsulateAttributeTransformation;
import spoon.reflect.code.CtFieldAccess;

public class AttributeEncapsulationOperator implements MigrationOperator {
    private TransformationProvider<CtFieldAccess> transformationProvider;
    private ElementTransformationTracker tracker;

    public AttributeEncapsulationOperator(TransformationProvider<CtFieldAccess> transformationProvider, ElementTransformationTracker tracker) {
        this.transformationProvider = transformationProvider;
        this.tracker = tracker;
    }

    @Override
    public void migrate(Migration migration) {
        String getterSignature = migration.mapping().context().get("getter").toString();
        String attributeSignature = migration.mapping().original().signature();
        transformationProvider.add(attributeSignature, new EncapsulateAttributeTransformation(tracker, attributeSignature, getterSignature));
//        transformer.addProcessor(statementTransformer.encapsulateAttribute(migration.mapping().original().signature(),getterSignature));
    }
}
