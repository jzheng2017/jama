package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Migration;
import nl.jiankai.api.TransformationProvider;
import nl.jiankai.spoon.transformations.clazz.ChangeReferenceTransformation;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class ClassReferenceOperator implements MigrationOperator {
    private final ElementTransformationTracker tracker;
    private final TransformationProvider<CtTypeReference> constructorTransformationProvider;
    private final Factory dependencyFactory;

    public ClassReferenceOperator(ElementTransformationTracker tracker, TransformationProvider<CtTypeReference> constructorTransformationProvider, Factory dependencyFactory) {
        this.tracker = tracker;
        this.constructorTransformationProvider = constructorTransformationProvider;
        this.dependencyFactory = dependencyFactory;
    }
    @Override
    public void migrate(Migration migration) {
        String originalSignature = migration.mapping().original().signature();
        String newSignature = migration.end().target().signature();
        constructorTransformationProvider.add(originalSignature, new ChangeReferenceTransformation(tracker, newSignature, dependencyFactory));
    }
}
