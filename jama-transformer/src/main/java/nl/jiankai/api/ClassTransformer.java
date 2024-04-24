package nl.jiankai.api;

public interface ClassTransformer<P> extends ElementHandler {

    P implementMethod(String fullyQualifiedClass, String methodSignature);

    P removeMethod(String fullyQualifiedClass, String methodSignature);

    P removeParent(String fullyQualifiedClass, String parentClass);
}
