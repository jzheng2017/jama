package nl.jiankai.api;

import nl.jiankai.api.project.Position;

import java.util.Optional;

public interface MethodQuery {
    Optional<Method> getMethod(String fullyQualifiedClassName, Position position);
}
