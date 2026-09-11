package site.ashenstation.infrastructure.config.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

public class JacksonRedisSerializer<T> implements RedisSerializer<T> {
    private final Class<T> clazz;
    private final ObjectMapper objectMapper;

    JacksonRedisSerializer(Class<T> clazz, ObjectMapper objectMapper) {
        this.clazz = clazz;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }

        try {
            return objectMapper.writeValueAsBytes(t);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Jackson 序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new SerializationException("Jackson 反序列化失败: " + e.getMessage(), e);
        }
    }
}
