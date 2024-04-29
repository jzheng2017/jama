package nl.jiankai.api;

import java.util.Set;

public interface StatementTransformer<P> extends ElementHandler {
    P encapsulateAttribute(String attributeSignature, String methodSignature);
    P handleException(String methodSignature, Set<String> exceptions);
}
