package nl.jiankai.api;

import java.util.Queue;
import java.util.stream.Stream;

public interface TransformationProvider<E> {

    Stream<Transformation<E>> consume(String id);

    void produce(String id, Transformation<E> transformer);
}
