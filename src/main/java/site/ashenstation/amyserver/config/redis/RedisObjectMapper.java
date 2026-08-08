package site.ashenstation.amyserver.config.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis 专用的 Jackson ObjectMapper
 * <p>
 * 开启多态类型信息(序列化时写入 {@code @class} 字段),用于从 Object 根类型恢复具体对象,
 * 等价于原 fastjson 的 WriteClassName + AutoType 白名单方案。
 * 仅允许反序列化白名单内的类型:
 * <ul>
 *     <li>{@code java.util} — Date / HashMap / ArrayList 等 JDK 容器类型</li>
 *     <li>{@code java.time} — LocalDateTime 等</li>
 *     <li>{@code site.ashenstation} — 项目业务类</li>
 * </ul>
 * <p>
 * ⚠️ 此 Mapper 仅供 Redis 序列化使用,禁止注册为 Spring Bean。
 * 一旦注册,Spring Boot 的 JacksonAutoConfiguration 会因 @ConditionalOnMissingBean 跳过默认配置,
 * 导致 Spring MVC 的 HTTP 消息转换器也使用该 Mapper,所有 HTTP 响应将泄漏 {@code @class} 字段。
 */
public final class RedisObjectMapper {

    private RedisObjectMapper() {
    }

    public static final ObjectMapper INSTANCE = create();

    private static ObjectMapper create() {
        // 多态类型白名单,与 fastjson 默认允许 JDK 类 + site.ashenstation 包的行为保持一致
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .allowIfSubType("site.ashenstation")
                .build();

        return new ObjectMapper()
                // 非 final 类型写入 @class 类型信息
                .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                // 与 fastjson 默认一致:忽略未知字段,保证反序列化宽松
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
