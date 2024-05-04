package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;

public class RemoveMethodCallArgumentTransformation implements Transformation<CtInvocation> {
    private final ElementTransformationTracker tracker;
    private final String methodSignature;
    private final int position;

    public RemoveMethodCallArgumentTransformation(ElementTransformationTracker tracker, String methodSignature, int position) {
        this.tracker = tracker;
        this.methodSignature = methodSignature;
        this.position = position;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        methodCall.removeArgument((CtExpression<?>) methodCall.getArguments().get(position));
        tracker.count(new TransformationEvent("Remove argument from method call", methodSignature));
    }
}
