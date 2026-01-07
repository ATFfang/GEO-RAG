package com.EarthCube.georag_backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. 手动创建并配置符合 4.0 规范的序列化器
        // 如果 RedisSerializer.json() 不接受 om，说明 4.0 倾向于使用内部默认的序列化逻辑
        // 但为了保证泛型正确，我们回归最本质且不被弃用的构造方式
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        // 2. 如果你需要深度定制 JSON（比如支持 Java8 时间），
        // 且 json() 无法传参，通常需要定义一个 @Primary 的 ObjectMapper Bean，
        // 或者直接在这里使用通用的 Jackson 包装

        // 3. 设置序列化规则
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        // 使用 4.0 推荐的静态工厂
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}