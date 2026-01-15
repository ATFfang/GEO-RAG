package com.EarthCube.georag_backend.config;

import com.EarthCube.georag_backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF (因为我们是用 Token 的，不需要 Session/Cookie 防护)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 设置为无状态 (不创建 HttpSession)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 核心：配置白名单 (URL 匹配规则)
                .authorizeHttpRequests(auth -> auth
                        // 允许所有的异步派发类型
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()

                        // ===============================================
                        // A. 🔓 这里的接口，所有人都能访问 (注册、登录、验证码)
                        // ===============================================
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // B. 🔓 Swagger 文档也不能拦截，否则前端没法看接口
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // C. 🤖 静态资源 (如果你有的话)
                        .requestMatchers("/static/**").permitAll()

                        // ===============================================
                        // D. 🛡️ 除了上面列出来的，其他所有请求必须登录
                        // ===============================================
                        .anyRequest().authenticated()
                )

                // 4. 把我们的 JWT 过滤器加到默认的用户名密码过滤器之前
                // 意思就是：先查 Token，Token 没问题了，再进去
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
