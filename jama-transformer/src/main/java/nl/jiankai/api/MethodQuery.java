package nl.jiankai.api;

import java.util.Optional;

public interface MethodQuery extends ElementHandler {
    Optional<Method> getMethod(String fullyQualifiedClassName, Position position);
}
