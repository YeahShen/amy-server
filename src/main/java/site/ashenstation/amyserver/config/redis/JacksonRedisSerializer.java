package site.ashenstation.amyserver.config.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

/**
 * Jackson Redis 序列化器
 * <p>
 * 基于 {@link RedisObjectMapper} 实现,序列化时写入 {@code @class} 类型信息,
 * 反序列化时通过白名单校验器恢复具体类型,替代原 FastJsonRedisSerializer。
 */
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
