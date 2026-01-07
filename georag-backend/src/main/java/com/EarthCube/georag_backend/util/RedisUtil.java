package com.EarthCube.georag_backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper; // Spring Boot 默认自动配置好的

    /**
     * 设置缓存（手动转JSON）
     */
    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, jsonValue, time, timeUnit);
        } catch (Exception e) {
            throw new RuntimeException("Redis序列化失败", e);
        }
    }

    /**
     * 获取缓存（手动转回对象）
     */
    public <T> T get(String key, Class<T> clazz) {
        String jsonValue = stringRedisTemplate.opsForValue().get(key);
        if (jsonValue == null) return null;
        try {
            return objectMapper.readValue(jsonValue, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Redis反序列化失败", e);
        }
    }

    // 基础操作不需要序列化
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public void del(String key) {
        stringRedisTemplate.delete(key);
    }
}