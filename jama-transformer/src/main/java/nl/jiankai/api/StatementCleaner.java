package nl.jiankai.api;

public interface StatementCleaner<P> extends ElementHandler<P> {
    P removeMethodCall(String methodSignature);
}
