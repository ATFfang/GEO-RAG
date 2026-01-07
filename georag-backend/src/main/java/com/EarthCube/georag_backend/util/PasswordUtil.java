package com.EarthCube.georag_backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具类
 * 依赖：spring-boot-starter-security 或 spring-security-crypto
 */
public class PasswordUtil {

    // BCryptPasswordEncoder 自带盐值生成，安全性极高
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 加密密码
     * @param rawPassword 前端传来的明文密码
     * @return 加密后的哈希字符串
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验密码
     * @param rawPassword 前端传来的明文密码
     * @param encodedPassword 数据库存的密文
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
