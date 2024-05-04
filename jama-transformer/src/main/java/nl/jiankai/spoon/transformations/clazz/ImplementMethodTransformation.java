package nl.jiankai.spoon.transformations.clazz;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import nl.jiankai.spoon.SpoonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;

import java.util.Collection;
import java.util.Optional;

public class ImplementMethodTransformation implements Transformation<CtClass> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImplementMethodTransformation.class);
    private final String methodSignature;
    private final ElementTransformationTracker tracker;

    public ImplementMethodTransformation(String methodSignature, ElementTransformationTracker tracker) {
        this.methodSignature = methodSignature;
        this.tracker = tracker;
    }

    @Override
    public void apply(CtClass ctClass) {
        Optional<CtMethod<?>> foundMethod = findMethod(ctClass.getAllMethods(), methodSignature);
        foundMethod.ifPresent(method -> {
            CtMethod<?> clone = method.clone();
            clone.removeModifier(ModifierKind.ABSTRACT);
            clone.setBody(ctClass.getFactory().createCtThrow("new UnsupportedOperationException(\"Implement this method\")"));
            LOGGER.info("Overriding/implementing method {}", methodSignature);
            ctClass.addMethod(clone);
            tracker.count(new TransformationEvent("Implement method", methodSignature));
        });
    }

    private Optional<CtMethod<?>> findMethod(Collection<CtMethod<?>> methods, String methodSignature) {
        return methods.stream().filter(method -> SpoonUtil.getSignature(method).equals(methodSignature)).findFirst();
    }
}
