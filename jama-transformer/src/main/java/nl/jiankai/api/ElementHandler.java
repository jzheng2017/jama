package nl.jiankai.api;

public interface ElementHandler<P> {
    default String name() {
        return this.getClass().getSimpleName();
    }

    P handle();
}
