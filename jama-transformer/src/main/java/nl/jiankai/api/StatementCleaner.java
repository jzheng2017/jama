package nl.jiankai.api;

public interface StatementCleaner<P> extends ElementHandler {
    P removeMethodCall(String methodSignature);
}
