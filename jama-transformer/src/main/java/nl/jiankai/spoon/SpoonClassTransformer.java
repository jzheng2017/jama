package nl.jiankai.spoon;

import nl.jiankai.api.ClassTransformer;
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
                    });
                });
            }
        };
    }
}
