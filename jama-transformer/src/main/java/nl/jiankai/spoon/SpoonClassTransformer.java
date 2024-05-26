package nl.jiankai.spoon;

import nl.jiankai.api.ElementHandler;
import nl.jiankai.api.TransformationProvider;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.declaration.CtClass;

public class SpoonClassTransformer implements ElementHandler<Processor<?>> {
    private TransformationProvider<CtClass> transformationProvider;

    public SpoonClassTransformer(TransformationProvider<CtClass> transformationProvider) {
        this.transformationProvider = transformationProvider;
    }

    @Override
    public Processor<CtClass> handle() {
        return new AbstractProcessor<>() {
            @Override
            public void process(CtClass ctClass) {
                transformationProvider
                        .get(SpoonUtil.getSignature(ctClass))
                        .forEach(transformation -> transformation.apply(ctClass));
            }
        };
    }
}
