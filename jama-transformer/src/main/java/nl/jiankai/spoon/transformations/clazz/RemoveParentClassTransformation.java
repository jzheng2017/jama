package nl.jiankai.spoon.transformations.clazz;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtClass;

public class RemoveParentClassTransformation implements Transformation<CtClass> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveParentClassTransformation.class);
    private final String fullyQualifiedClass;
    private final String parentClass;
    private final ElementTransformationTracker tracker;

    public RemoveParentClassTransformation(String fullyQualifiedClass, String parentClass, ElementTransformationTracker tracker) {
        this.fullyQualifiedClass = fullyQualifiedClass;
        this.parentClass = parentClass;
        this.tracker = tracker;
    }

    @Override
    public void apply(CtClass ctClass) {
        if (ctClass.getSuperclass() != null && ctClass.getSuperclass().getSimpleName().equals(parentClass)) {
            LOGGER.info("Removing super class '{}' from class '{}'", parentClass, fullyQualifiedClass);
            tracker.count(new TransformationEvent("Removing super class", fullyQualifiedClass));
            ctClass.setSuperclass(null);
        } else {
            ctClass
                    .getSuperInterfaces()
                    .stream()
                    .filter(parent -> parent.getSimpleName().equals(parentClass))
                    .findFirst()
                    .ifPresent(ref -> {
                        LOGGER.info("Removing parent interface '{}' from class '{}'", parentClass, fullyQualifiedClass);
                        ctClass.removeSuperInterface(ref);
                        tracker.count(new TransformationEvent("Removing parent interface", fullyQualifiedClass));
                    });
        }
    }
}
