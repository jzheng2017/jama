package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import spoon.reflect.code.CtInvocation;

public class AddMethodCallArgumentTransformation<T> implements Transformation<CtInvocation> {
    private final ElementTransformationTracker tracker;
    private final T value;
    private final String methodSignature;
    private final int position;

    public AddMethodCallArgumentTransformation(ElementTransformationTracker tracker, T value, String methodSignature, int position) {
        this.tracker = tracker;
        this.value = value;
        this.methodSignature = methodSignature;
        this.position = position;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        boolean addToEndOfList = position == -1;

        if (addToEndOfList) {
            methodCall.addArgument(methodCall.getFactory().Code().createLiteral(value));
        } else {
            methodCall.addArgumentAt(position, methodCall.getFactory().Code().createLiteral(value));
        }

        tracker.count(new TransformationEvent("Add argument to method call", methodSignature));
    }
}
