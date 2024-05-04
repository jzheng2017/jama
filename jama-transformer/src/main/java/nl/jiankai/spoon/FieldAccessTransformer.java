package nl.jiankai.spoon;

import nl.jiankai.api.ElementHandler;
import nl.jiankai.api.TransformationProvider;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.code.*;

public class FieldAccessTransformer implements ElementHandler<Processor<?>> {

    private final TransformationProvider<CtFieldAccess> transformationProvider;

    public FieldAccessTransformer(TransformationProvider<CtFieldAccess> transformationProvider) {
        this.transformationProvider = transformationProvider;
    }

    @Override
    public Processor<?> handle() {
        return new AbstractProcessor<CtFieldAccess>() {
            @Override
            public void process(CtFieldAccess fieldAccess) {
                transformationProvider
                        .consume(SpoonUtil.getSignature(fieldAccess))
                        .forEach(transformation -> transformation.apply(fieldAccess));
            }
        };
    }
}
