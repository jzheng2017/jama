package nl.jiankai.api;

public interface ElementHandler {
    default String name() {
        return this.getClass().getSimpleName();
    }
}
