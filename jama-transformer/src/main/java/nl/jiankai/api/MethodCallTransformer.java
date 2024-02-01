package nl.jiankai.api;

public interface MethodCallTransformer {
    void rename(Method original, String newName);
    void changeReference(Method original, String newPath);
}
