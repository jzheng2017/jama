package nl.jiankai.api;

public interface StatementTransformer<P> extends ElementHandler {
    P encapsulateAttribute(String attributeSignature, String methodSignature);
}
