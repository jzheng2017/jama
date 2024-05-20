package nl.jiankai.spoon.transformations.method;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class ChangeMethodCallReferenceTransformation implements Transformation<CtInvocation> {
    private final ElementTransformationTracker tracker;
    private final String methodSignature;
    private final Factory dependencyFactory;

    public ChangeMethodCallReferenceTransformation(ElementTransformationTracker tracker, String methodSignature, Factory dependencyFactory) {
        this.tracker = tracker;
        this.methodSignature = methodSignature;
        this.dependencyFactory = dependencyFactory;
    }

    @Override
    public void apply(CtInvocation methodCall) {
        String methodSignature = this.methodSignature;

        if (methodSignature.contains("#")) {
            methodSignature = methodSignature.substring(0, methodSignature.lastIndexOf("#"));
        }

        CtTypeReference<?> ref =
                dependencyFactory
                        .Type()
                        .get(methodSignature)
                        .getReference();

        methodCall.getExecutable().setDeclaringType(ref);
        if (methodCall.getTarget() instanceof CtTypeAccess target) { //static method
            target.setAccessedType(ref);
        } else {
            methodCall.getTarget().setType(ref);
        }
        tracker.count(new TransformationEvent("Changing method call reference", methodSignature), methodCall.getPosition().getFile().getAbsolutePath());
    }
}
