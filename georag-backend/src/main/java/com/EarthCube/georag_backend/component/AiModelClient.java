package com.EarthCube.georag_backend.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型服务客户端
 * 负责封装与 Python 服务的所有 HTTP 通信细节
 */
@Slf4j
@Component
public class AiModelClient {

    @Autowired
    private WebClient webClient;

    /**
     * 发起流式对话请求
     *
     * @param query   用户当前的问题
     * @param history 历史上下文（已包含用户刚才的问题）
     * @return 响应式文本流 (Flux<String>)，每个元素是一个 Token
     */
    public Flux<String> streamChat(String query, List<Map<String, String>> history) {
        // 1. 构建符合 Python 接口要求的 Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("history", history);

        log.info("正在调用 AI 服务, Query长度: {}, History条数: {}", query.length(), history.size());

        // 2. 发起非阻塞调用
        return webClient.post()
                .uri("/chat/stream") // 对应 Python 的接口路径
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .accept(MediaType.TEXT_EVENT_STREAM) // 声明接收流
                .retrieve()
                .bodyToFlux(String.class); // 自动将流转换为 Flux<String>
    }
}