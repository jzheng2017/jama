package nl.jiankai.spoon;

import nl.jiankai.api.ElementHandler;
import nl.jiankai.api.TransformationProvider;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtInvocation;

import static nl.jiankai.spoon.SpoonUtil.getSignature;

public class SpoonMethodCallTransformer implements ElementHandler<Processor<?>> {
    private final TransformationProvider<CtInvocation> transformationProvider;

    public SpoonMethodCallTransformer(TransformationProvider<CtInvocation> transformationProvider) {
        this.transformationProvider = transformationProvider;
    }

    @Override
    public Processor<?> handle() {
        return new AbstractProcessor<CtInvocation>() {
            @Override
            public void process(CtInvocation methodCall) {
                transformationProvider
                        .get(getSignature(methodCall))
                        .forEach(transformation -> transformation.apply(methodCall));
            }
        };
    }
}
