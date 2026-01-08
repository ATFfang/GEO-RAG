package com.EarthCube.georag_backend.security;

import com.EarthCube.georag_backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 推荐加上日志
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService; // Spring Security 的标准接口

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 从请求头获取 Token
            String token = getTokenFromRequest(request);

            // 2. 如果 Token 存在
            if (StringUtils.hasText(token)) {

                // 3. 解析 Token (这里会利用你的 util 验签)
                // 注意：如果 Token 过期或签名不对，parseToken 会直接抛出 JwtException 异常
                Claims claims = jwtUtil.parseToken(token);

                // 4. 根据你的 createToken 逻辑，用户名存在 "username" 这个 claim 里
                String username = claims.get("username", String.class);

                // 如果你想获取 userId，可以调用 claims.getSubject();

                // 5. 如果解析成功且当前上下文未认证
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 6. 加载数据库中的用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 7. 再次校验过期时间 (虽然 parseToken 实际上已经校验过了，但为了稳健可以保留)
                    if (!jwtUtil.isTokenExpired(token)) {

                        // 8. 构建认证对象
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 9. 放入 SecurityContext，表示“已登录”
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            // 这里捕获所有 Token 解析异常 (过期、篡改、格式错误)
            // 不要抛出异常，而是记录日志，让请求继续走（Security 会拦截到它是未认证的）
            log.error("Token鉴权失败: {}", e.getMessage());
        }

        // 10. 放行，进入下一个过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 辅助方法：从 Header 提取 Bearer Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}