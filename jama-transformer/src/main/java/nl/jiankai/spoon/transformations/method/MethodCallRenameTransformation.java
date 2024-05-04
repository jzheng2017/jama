package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtInvocation;

/**
 * Renames the method name of a method call
 */
public class MethodCallRenameTransformation implements Transformation<CtInvocation> {
    private final Logger LOGGER = LoggerFactory.getLogger(MethodCallRenameTransformation.class);
    private final ElementTransformationTracker tracker;
    private final String newName;
    private final String originalSignature;

    public MethodCallRenameTransformation(ElementTransformationTracker tracker, String newName, String originalSignature) {
        this.tracker = tracker;
        this.newName = newName;
        this.originalSignature = originalSignature;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        methodCall.getExecutable().setSimpleName(newName);
        tracker.count(new TransformationEvent("Renaming method call", originalSignature));
    }
}
