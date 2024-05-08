package nl.jiankai.api;

import java.util.Map;

public interface SerializationService {
    byte[] serialize(Object object);

    <T> T deserialize(byte[] object, Class<T> deserializedClass);
    Map<String, Object> read(byte[] object);
}
