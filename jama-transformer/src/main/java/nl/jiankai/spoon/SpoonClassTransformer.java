package nl.jiankai.spoon;

import nl.jiankai.api.ClassTransformer;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;

import java.util.Optional;

public class SpoonClassTransformer implements ClassTransformer<Processor<CtClass<?>>> {
    @Override
    public Processor<CtClass<?>> implementMethod(String fullyQualifiedClass, String methodSignature) {
        return new AbstractProcessor<>() {
            @Override
            public void process(CtClass<?> ctClass) {
                if (ctClass.getReference().getQualifiedName().equals(fullyQualifiedClass)) {
                    Optional<CtMethod<?>> first = ctClass.getAllMethods().stream().filter(method -> SpoonUtil.getSignature(method).equals(methodSignature)).findFirst();
                    first.ifPresent(method -> {
                        CtMethod<?> clone = method.clone();
                        clone.removeModifier(ModifierKind.ABSTRACT);
                        clone.setBody(getFactory().createCtThrow("new UnsupportedOperationException(\"Implement this method\")"));
                        ctClass.addMethod(clone);
                    });
                }
            }
        };
    }
}
