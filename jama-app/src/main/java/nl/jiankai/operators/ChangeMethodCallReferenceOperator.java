package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.*;
import nl.jiankai.spoon.transformations.method.ChangeMethodCallReferenceTransformation;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.factory.Factory;

import java.util.Set;

public class ChangeMethodCallReferenceOperator implements MigrationOperator {
    private final ElementTransformationTracker tracker;
    private final TransformationProvider<CtInvocation> transformationProvider;
    private final Factory dependencyFactory;
    public ChangeMethodCallReferenceOperator(ElementTransformationTracker tracker, TransformationProvider<CtInvocation> transformationProvider, Factory dependencyFactory) {
        this.tracker = tracker;
        this.transformationProvider = transformationProvider;
        this.dependencyFactory = dependencyFactory;
    }

    @Override
    public void migrate(Migration migration) {
        Set<RefactoringType> refactoringTypes = migration.refactorings();
        String originalSignature = migration.mapping().original().signature();

        if (refactoringTypes.contains(RefactoringType.MOVE_METHOD)) {
            transformationProvider.produce(originalSignature, new ChangeMethodCallReferenceTransformation(tracker, originalSignature, dependencyFactory));
        }
    }
}
