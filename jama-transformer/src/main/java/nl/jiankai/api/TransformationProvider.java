package nl.jiankai.api;

import java.util.stream.Stream;

public interface TransformationProvider<E> {

    Stream<Transformation<E>> get(String id);

    void add(String id, Transformation<E> transformer);
}
