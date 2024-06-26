package nl.jiankai.api.storage;

import nl.jiankai.api.Identifiable;

import java.util.Optional;

public interface CacheService<T extends Identifiable> {

    boolean isCached(String identifier);

    default boolean isCached(T entity) {
        return isCached(entity.getId());
    }

    Optional<T> get(String identifier);

    void write(T entity);

    void clear();
}
