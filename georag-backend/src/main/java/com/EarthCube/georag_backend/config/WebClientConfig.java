package com.EarthCube.georag_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // 这里配置 Python 服务的基准地址
        return WebClient.builder()
                .baseUrl("http://localhost:8000")
                .build();
    }
}
