package com.EarthCube.georag_backend.interceptor;

import com.EarthCube.georag_backend.common.context.UserContext;
import com.EarthCube.georag_backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        // 1. 如果不是映射到方法直接通过（比如静态资源，或者 OPTIONS 请求）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 2. 从 Header 中获取 Token
        String token = request.getHeader("Authorization");

        // 3. 校验 Token 格式 (必须以 "Bearer " 开头)
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        // 去掉 "Bearer " 前缀
        token = token.substring(7);

        try {
            // 4. 解析 Token (如果过期或签名不对，这里会抛出异常)
            Claims claims = jwtUtil.parseToken(token);

            // 5. 获取 userId (你在 JwtUtil.createToken 里是用 setSubject 存的)
            String userId = claims.getSubject();

            // 6. 存入 ThreadLocal
            if (StringUtils.hasText(userId)) {
                UserContext.setUserId(userId);
                return true;
            }
        } catch (Exception e) {
            log.warn("Token认证失败: {}", e.getMessage());
        }

        // 7. 失败返回 401
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
        // 8. 请求结束，务必清理线程变量，防止内存泄漏
        UserContext.removeUserId();
    }
}
