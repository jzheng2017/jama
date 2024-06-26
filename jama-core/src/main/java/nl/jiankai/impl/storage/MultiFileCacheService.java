package nl.jiankai.impl.storage;

import nl.jiankai.api.Identifiable;
import nl.jiankai.api.SerializationService;
import nl.jiankai.api.storage.CacheService;
import nl.jiankai.impl.HashingUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MultiFileCacheService<T extends Identifiable> implements CacheService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiFileCacheService.class);
    private final String baseLocation;
    private final SerializationService serializationService;
    private final Map<String, T> cache = new HashMap<>();
    private final Class<T> entityClassType;

    public MultiFileCacheService(String baseLocation, SerializationService serializationService, Class<T> entityClassType) {
        this.baseLocation = baseLocation;
        this.serializationService = serializationService;
        this.entityClassType = entityClassType;
    }

    @Override
    public boolean isCached(String identifier) {
        return cache.containsKey(identifier) || new LocalFileStorageService(createFileLocation(identifier), false).exists();
    }

    @Override
    public Optional<T> get(String identifier) {
        if (cache.containsKey(identifier)) {
            LOGGER.info("'{}' is cached. Trying to fetch from cache..", identifier);
            return Optional.of(cache.get(identifier));
        } else {
            LOGGER.info("'{}' is not cached in memory.. Trying to fetch from disk..", identifier);
            LocalFileStorageService fileStorageService = new LocalFileStorageService(createFileLocation(identifier), false);
            if (fileStorageService.exists()) {
                LOGGER.info("'{}' found on the disk cache!", identifier);
                return Optional.of(
                        serializationService.deserialize(
                                fileStorageService.read().collect(Collectors.joining()).getBytes(),
                                entityClassType
                        )
                );
            }
        }

        LOGGER.info("'{}' could not be found in the memory or disk cache..", identifier);
        return Optional.empty();
    }

    @Override
    public void write(T entity) {
        LocalFileStorageService fileStorageService = new LocalFileStorageService(createFileLocation(entity.getId()), true);
        cache.put(entity.getId(), entity);
        fileStorageService.write(new String(serializationService.serialize(entity)));
        LOGGER.debug("Written entity '{}' to the cache", entity.getId());
    }

    @Override
    public void clear() {
        try {
            cache.clear();
            FileUtils.deleteDirectory(new File(baseLocation));
            LOGGER.info("Cache at location '{}' has been cleared", baseLocation);
        } catch (IOException e) {
            LOGGER.warn("Could not clear cache at location '{}'", baseLocation, e);
        }
    }


    private String createFileLocation(String filename) {
        try {
            return baseLocation + File.separator + HashingUtil.md5Hash(filename) + "." + serializationService.extension();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Couldn't hash the filename due to the hashing algorithm not being present", e);
            throw new IllegalStateException(e);
        }
    }
}
