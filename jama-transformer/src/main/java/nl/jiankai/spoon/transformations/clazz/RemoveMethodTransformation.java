package nl.jiankai.spoon.transformations.clazz;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import nl.jiankai.spoon.SpoonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

import java.util.Collection;
import java.util.Optional;

public class RemoveMethodTransformation implements Transformation<CtClass> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveMethodTransformation.class);
    private final String methodSignature;
    private final ElementTransformationTracker tracker;

    public RemoveMethodTransformation(String methodSignature, ElementTransformationTracker tracker) {
        this.methodSignature = methodSignature;
        this.tracker = tracker;
    }

    @Override
    public void apply(CtClass ctClass) {
        Optional<CtMethod<?>> foundMethod = findMethod(ctClass.getMethods(), methodSignature);
        foundMethod.ifPresent(method -> {
            LOGGER.info("Removing method {}", methodSignature);
            ctClass.removeMethod(method);
            tracker.count(new TransformationEvent("Remove method", methodSignature), ctClass.getPosition().getFile().getAbsolutePath());
        });
    }
    private Optional<CtMethod<?>> findMethod(Collection<CtMethod<?>> methods, String methodSignature) {
        return methods.stream().filter(method -> SpoonUtil.getSignature(method).equals(methodSignature)).findFirst();
    }
}
