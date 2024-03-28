package nl.jiankai.api;

public interface Transformer<P> {
    void addProcessor(P processor);
    void run();
}
