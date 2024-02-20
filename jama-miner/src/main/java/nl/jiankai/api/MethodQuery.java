package nl.jiankai.api;

import java.io.File;
import java.util.Optional;

public interface MethodQuery {
    Optional<Method> getMethod(String fullyQualifiedClassName, Position position);
}
