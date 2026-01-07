package com.EarthCube.georag_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static io.jsonwebtoken.Jwts.*;

@Component
public class JwtUtil {

    // 密钥：在真实生产环境中，请确保该值足够复杂且存储在环境变量中
    // 这里设置一个默认值防止启动报错，你可以在 application.yaml 中配置 jwt.secret
    @Value("${jwt.secret:EarthCube_GeoRag_Backend_Secret_Key_For_2026_Project_Security}")
    private String secret;

    // 过期时间：默认 7 天 (单位：毫秒)
    @Value("${jwt.expiration:604800000}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String createToken(String userId, String username) {
        return builder()
                .setSubject(userId)
                .claim("username", username) // 可以放入更多非敏感信息
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token 获取 Claims
     * @param token JWT 字符串
     * @return Claims 载荷
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 校验 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}