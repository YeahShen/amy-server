package site.ashenstation.utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

public class RedisObjectMapper {

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
