package nl.jiankai.spoon;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.ElementHandler;
import nl.jiankai.api.TransformationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                        .consume(SpoonUtil.getSignature(ctClass))
                        .forEach(transformation -> transformation.apply(ctClass));
            }
        };
    }

}
