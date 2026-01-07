package com.EarthCube.georag_backend.config;

import com.EarthCube.georag_backend.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦截所有 API 路径
                .addPathPatterns("/**")
                // 放行白名单
                .excludePathPatterns(
                        "/api/v1/users/login",              // 登录
                        "/api/v1/users/register",           // 注册
                        "/api/v1/users/send-register-code", // 发送验证码
                        "/error",                           // Spring Boot 默认错误页
                        "/doc.html",                        // Swagger/Knife4j
                        "/webjars/**",
                        "/swagger-resources",
                        "/v3/api-docs/**"
                );
    }
}
