package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import spoon.reflect.code.CtInvocation;

import java.util.Collections;

public class SwapMethodCallArgumentsTransformation implements Transformation<CtInvocation> {
    private final ElementTransformationTracker tracker;
    private final String methodSignature;
    private final int positionArgument;
    private final int positionArgument2;

    public SwapMethodCallArgumentsTransformation(ElementTransformationTracker tracker, String methodSignature, int positionArgument, int positionArgument2) {
        this.tracker = tracker;
        this.methodSignature = methodSignature;
        this.positionArgument = positionArgument;
        this.positionArgument2 = positionArgument2;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        Collections.swap(methodCall.getArguments(), positionArgument, positionArgument2);
        tracker.count(new TransformationEvent("Swapping arguments method call", methodSignature));
    }
}
