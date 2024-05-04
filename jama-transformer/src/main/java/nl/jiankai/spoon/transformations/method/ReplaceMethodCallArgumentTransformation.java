package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;

public class ReplaceMethodCallArgumentTransformation<T> implements Transformation<CtInvocation> {
    private final ElementTransformationTracker tracker;
    private final String methodSignature;
    private final int position;
    private final T value;

    public ReplaceMethodCallArgumentTransformation(ElementTransformationTracker tracker, String methodSignature, int position, T value) {
        this.tracker = tracker;
        this.methodSignature = methodSignature;
        this.position = position;
        this.value = value;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        CtExpression<?> expression = (CtExpression<?>) methodCall.getArguments().get(position);
        expression.replace(methodCall.getFactory().Code().createLiteral(value));
        tracker.count(new TransformationEvent("Replacing argument method call", methodSignature));
    }
}
