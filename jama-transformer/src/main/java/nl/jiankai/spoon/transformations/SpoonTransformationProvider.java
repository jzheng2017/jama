package nl.jiankai.spoon.transformations;

import nl.jiankai.api.Transformation;
import nl.jiankai.api.TransformationProvider;
import spoon.reflect.declaration.CtElement;

import java.util.*;
import java.util.stream.Stream;

public class SpoonTransformationProvider<E extends CtElement> implements TransformationProvider<E> {
    private final Map<String, Queue<Transformation<E>>> transformationsPerSignature = new HashMap<>();

    @Override
    public Stream<Transformation<E>> consume(String id) {
        if (!transformationsPerSignature.containsKey(id)) {
            return Stream.empty();
        }

        return transformationsPerSignature.get(id).stream();
    }

    @Override
    public void produce(String id, Transformation<E> transformation) {
        transformationsPerSignature
                .computeIfAbsent(id, k -> new LinkedList<>())
                .add(transformation);
    }
}
