package nl.jiankai.api;

public interface MethodCallTransformer {
    void rename(Method original, Method target);
}
