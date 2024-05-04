package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.*;
import nl.jiankai.spoon.transformations.method.MethodCallRenameTransformation;
import spoon.reflect.code.CtInvocation;

public class RenameMethodCallOperator implements MigrationOperator {
    private final TransformationProvider<CtInvocation> transformationProvider;
    private final ElementTransformationTracker tracker;

    public RenameMethodCallOperator(TransformationProvider<CtInvocation> transformationProvider, ElementTransformationTracker tracker) {
        this.transformationProvider = transformationProvider;
        this.tracker = tracker;
    }

    @Override
    public void migrate(Migration migration) {
        String originalSignature = migration.mapping().original().signature();
        String finalName = migration.end().target().name();

        transformationProvider.produce(originalSignature, new MethodCallRenameTransformation(tracker, finalName, originalSignature));
    }
}
