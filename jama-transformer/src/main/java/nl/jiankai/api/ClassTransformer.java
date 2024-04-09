package nl.jiankai.api;

public interface ClassTransformer<P> {

    P implementMethod(String fullyQualifiedClass, String methodSignature);

    P removeMethod(String fullyQualifiedClass, String methodSignature);
}
