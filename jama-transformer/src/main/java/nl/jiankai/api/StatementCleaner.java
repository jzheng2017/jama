package nl.jiankai.api;

public interface StatementCleaner<P> {
    P removeMethodCall(String methodSignature);
}
