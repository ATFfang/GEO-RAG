package com.EarthCube.georag_backend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 普通缓存获取
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param time 时间(秒) time > 0 若 time <= 0 将设置无限期
     */
    public void set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取过期时间
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 获取 List 缓存的内容 (用于获取上下文)
     * @param key 键
     * @param start 开始索引 (0)
     * @param end 结束索引 (-1 代表所有)
     * @return List<Object>
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将消息放入 List 缓存 (用于追加新消息)
     * @param key 键
     * @param value 值
     * @return boolean
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保持 List 长度 (例如只保留最近 20 条，防止 Token 溢出)
     * @param key 键
     * @param count 保留的条数（保留最新的 count 条）
     */
    public void lTrim(String key, long count) {
        try {
            // LTRIM key start stop
            // 保留最新的 count 条，在 Redis 中对应索引区间为: [size-count, -1]
            // 注意：Redis 的 trim 逻辑是保留区间内的元素，删除区间外的。
            // 简单做法：直接保留 0 到 -1 是全量。
            // 这是一个 FIFO 队列，右进左出？不对，通常是右进，获取时也是 range 0 -1。
            // 为了保留最新的 N 条，我们应该保留索引 [ -count, -1 ]

            // 修正逻辑：
            // 如果我们一直用 rightPush，那么 index 0 是最旧的，index -1 是最新的。
            // 我们想保留最新的 20 条，即保留 [size-20, size-1]。
            // 对应的 Redis 命令是 LTRIM key -20 -1

            redisTemplate.opsForList().trim(key, -count, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     * @return boolean
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}