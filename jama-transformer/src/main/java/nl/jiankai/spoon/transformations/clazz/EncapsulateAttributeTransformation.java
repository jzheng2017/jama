package nl.jiankai.spoon.transformations.clazz;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import nl.jiankai.spoon.SpoonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

import java.util.Optional;

public class EncapsulateAttributeTransformation implements Transformation<CtFieldAccess> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncapsulateAttributeTransformation.class);
    private final ElementTransformationTracker tracker;
    private final String attributeSignature;
    private final String methodSignature;

    public EncapsulateAttributeTransformation(ElementTransformationTracker tracker, String attributeSignature, String methodSignature) {
        this.tracker = tracker;
        this.attributeSignature = attributeSignature;
        this.methodSignature = methodSignature;
    }

    @Override
    public void apply(CtFieldAccess element) {
        String attributeQualifiedName = element.getVariable().getQualifiedName();
        if (attributeSignature.equals(attributeQualifiedName)) {
            CtClass<?> methodDeclaredClass = (CtClass<?>) element.getVariable().getFieldDeclaration().getParent();
            Optional<CtMethod<?>> methodOptional = methodDeclaredClass.getAllMethods().stream().filter(m -> SpoonUtil.getSignature(m).equals(methodSignature)).findFirst();
            methodOptional.ifPresentOrElse(
                    method -> {
                        LOGGER.info("Encapsulating variable read access '{}' with method '{}'", attributeSignature, methodSignature);
                        element.replace(element.getFactory().createInvocation(element.getTarget(), method.getReference()));
                        tracker.count(new TransformationEvent("Encapsulate attribute", attributeQualifiedName), element.getPosition().getFile().getAbsolutePath());
                    },
                    () -> LOGGER.warn("Failed to encapsulate variable access '{}' as method '{}' could not be found", attributeSignature, methodSignature)
            );
        }
    }
}
