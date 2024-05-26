package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.*;
import nl.jiankai.spoon.transformations.method.RenameMethodCallTransformation;
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
        String originalSignature = SignatureUtil.getFirstSignature(migration.mapping().original().signature(), tracker);
        String finalName = migration.end().target().name();

        transformationProvider.add(originalSignature, new RenameMethodCallTransformation(tracker, finalName, originalSignature));
    }
}
