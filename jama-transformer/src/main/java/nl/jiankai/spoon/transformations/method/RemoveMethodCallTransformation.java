package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import nl.jiankai.spoon.SpoonUtil;
import nl.jiankai.util.TypeUtil;
import spoon.reflect.code.CtInvocation;

public class RemoveMethodCallTransformation implements Transformation<CtInvocation> {
    private final String methodSignature;
    private final ElementTransformationTracker tracker;

    public RemoveMethodCallTransformation(String methodSignature, ElementTransformationTracker tracker) {
        this.methodSignature = methodSignature;
        this.tracker = tracker;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        String signature = SpoonUtil.getSignatureWithoutClass(methodCall);

        if (signature.equals(methodSignature)) {
            methodCall.replace(
                    methodCall
                            .getFactory()
                            .Code()
                            .createLiteral(
                                    TypeUtil.getDefaultValue(SpoonUtil.inferMethodCallType(methodCall))
                            )
            );
            tracker.count(new TransformationEvent("Removing method call", methodSignature), methodCall.getPosition().getFile().getAbsolutePath());
        }
    }
}
