package nl.jiankai.spoon;

import nl.jiankai.api.ClassTransformer;
import nl.jiankai.api.ElementTransformationTracker;
import nl.jiankai.api.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;

import java.util.Collection;
import java.util.Optional;

public class SpoonClassTransformer implements ClassTransformer<Processor<CtClass<?>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoonClassTransformer.class);
    private final ElementTransformationTracker tracker;

    public SpoonClassTransformer(ElementTransformationTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public Processor<CtClass<?>> implementMethod(String fullyQualifiedClass, String methodSignature) {
        return new AbstractProcessor<>() {
            @Override
            public void process(CtClass<?> ctClass) {
                executeIfClassMatches(ctClass, fullyQualifiedClass, () -> {
                    Optional<CtMethod<?>> foundMethod = findMethod(ctClass.getAllMethods(), methodSignature);
                    foundMethod.ifPresent(method -> {
                        CtMethod<?> clone = method.clone();
                        clone.removeModifier(ModifierKind.ABSTRACT);
                        clone.setBody(getFactory().createCtThrow("new UnsupportedOperationException(\"Implement this method\")"));
                        LOGGER.info("Overriding/implementing method {}", methodSignature);
                        ctClass.addMethod(clone);
                        tracker.count(new Transformation("Implement method", methodSignature));
                    });
                });
            }
        };
    }

    private static void executeIfClassMatches(CtClass<?> ctClass, String fullyQualifiedClass, Runnable action) {
        if (ctClass.getReference().getQualifiedName().equals(fullyQualifiedClass)) {
            action.run();
        }
    }

    private static Optional<CtMethod<?>> findMethod(Collection<CtMethod<?>> methods, String methodSignature) {
        return methods.stream().filter(method -> SpoonUtil.getSignature(method).equals(methodSignature)).findFirst();
    }

    @Override
    public Processor<CtClass<?>> removeMethod(String fullyQualifiedClass, String methodSignature) {
        return new AbstractProcessor<>() {
            @Override
            public void process(CtClass<?> ctClass) {
                executeIfClassMatches(ctClass, fullyQualifiedClass, () -> {
                    Optional<CtMethod<?>> foundMethod = findMethod(ctClass.getMethods(), methodSignature);
                    foundMethod.ifPresent(method -> {
                        LOGGER.info("Removing method {}", methodSignature);
                        ctClass.removeMethod(method);
                        tracker.count(new Transformation("Remove method", methodSignature));
                    });
                });
            }
        };
    }

    @Override
    public Processor<CtClass<?>> removeParent(String fullyQualifiedClass, String parentClass) {
        return new AbstractProcessor<>() {
            @Override
            public void process(CtClass<?> ctClass) {
                executeIfClassMatches(ctClass, fullyQualifiedClass, () -> {
                    if (ctClass.getSuperclass() != null && ctClass.getSuperclass().getSimpleName().equals(parentClass)) {
                        LOGGER.info("Removing super class '{}' from class '{}'", parentClass, fullyQualifiedClass);
                        tracker.count(new Transformation("Removing super class", fullyQualifiedClass));
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
                                    tracker.count(new Transformation("Removing parent interface", fullyQualifiedClass));
                                });
                    }
                });
            }
        };
    }
}
