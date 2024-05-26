package nl.jiankai.spoon;

import nl.jiankai.ElementTransformationTracker;
import nl.jiankai.api.ElementHandler;
import nl.jiankai.api.TransformationProvider;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.CtInvocation;

import static nl.jiankai.spoon.SpoonUtil.getSignature;

public class SpoonMethodCallTransformer implements ElementHandler<Processor<?>> {
    private final TransformationProvider<CtInvocation> transformationProvider;
    private final ElementTransformationTracker tracker;
    public SpoonMethodCallTransformer(TransformationProvider<CtInvocation> transformationProvider, ElementTransformationTracker tracker) {
        this.transformationProvider = transformationProvider;
        this.tracker = tracker;
    }

    @Override
    public Processor<?> handle() {
        return new AbstractProcessor<CtInvocation>() {
            @Override
            public void process(CtInvocation methodCall) {
                transformationProvider
                        .get(getSignature(methodCall, tracker))
                        .forEach(transformation -> transformation.apply(methodCall));

                //if changes happened to the class then the reference changed, so it must be updated
                transformationProvider
                        .get(SpoonUtil.getClass(methodCall, tracker))
                        .forEach(transformation -> transformation.apply(methodCall));
            }
        };
    }
}
