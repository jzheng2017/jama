package nl.jiankai.spoon;

import nl.jiankai.api.ElementHandler;
import nl.jiankai.api.TransformationProvider;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.reference.CtTypeReference;

public class SpoonReferenceTransformer implements ElementHandler<Processor<?>> {
    private final TransformationProvider<CtTypeReference> transformationProvider;

    public SpoonReferenceTransformer(TransformationProvider<CtTypeReference> transformationProvider) {
        this.transformationProvider = transformationProvider;
    }

    @Override
    public Processor<?> handle() {
        return new AbstractProcessor<CtTypeReference>() {
            @Override
            public void process(CtTypeReference typeReference) {
                transformationProvider
                        .get(typeReference.getQualifiedName())
                        .forEach(transformation -> transformation.apply(typeReference));
            }
        };
    }
}
