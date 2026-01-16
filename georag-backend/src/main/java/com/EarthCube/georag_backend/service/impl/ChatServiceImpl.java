package com.EarthCube.georag_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.EarthCube.georag_backend.common.context.UserContext;
import com.EarthCube.georag_backend.common.exception.BusinessException;
import com.EarthCube.georag_backend.component.AiModelClient;
import com.EarthCube.georag_backend.dto.chat.*;
import com.EarthCube.georag_backend.entity.ChatMessage;
import com.EarthCube.georag_backend.entity.ChatSession;
import com.EarthCube.georag_backend.mapper.ChatMessageMapper;
import com.EarthCube.georag_backend.mapper.ChatSessionMapper;
import com.EarthCube.georag_backend.service.IChatService;
import com.EarthCube.georag_backend.util.RedisUtil;
import com.EarthCube.georag_backend.vo.chat.ChatMessageVO;
import com.EarthCube.georag_backend.vo.chat.ChatSessionVO;
import com.EarthCube.georag_backend.vo.chat.ChatStreamVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements IChatService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AiModelClient aiModelClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_CONTEXT_COUNT = 20; // 只保留最近20轮对话
    private static final long CONTEXT_TTL = 1800;    // 30分钟无操作清除缓存

    // 专门用于 SSE 推送的线程池
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    private String getCurrentUserId() {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new BusinessException("用户未登录");
        }
        return userIdStr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(ChatSessionCreateDTO dto) {
        String userId = getCurrentUserId();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(StrUtil.isBlank(dto.getTitle()) ? "新对话" : dto.getTitle());
        session.setMetaInfo(dto.getMetaInfo());
        session.setStatus(1);
        chatSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public Page<ChatSessionVO> getSessionList(ChatSessionQueryDTO queryDTO) {
        String userId = getCurrentUserId();
        Page<ChatSession> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId);
        wrapper.eq(ChatSession::getStatus, 1);
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            wrapper.like(ChatSession::getTitle, queryDTO.getKeyword());
        }
        wrapper.orderByDesc(ChatSession::getUpdateTime);
        chatSessionMapper.selectPage(page, wrapper);
        Page<ChatSessionVO> resultPage = new Page<>();
        BeanUtil.copyProperties(page, resultPage, "records");
        List<ChatSessionVO> voList = page.getRecords().stream().map(session -> {
            ChatSessionVO vo = new ChatSessionVO();
            BeanUtil.copyProperties(session, vo);
            return vo;
        }).collect(Collectors.toList());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSession(String sessionId, ChatSessionUpdateDTO dto) {
        checkSessionOwner(sessionId);
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setTitle(dto.getTitle());
        chatSessionMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        checkSessionOwner(sessionId);
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setStatus(2);
        chatSessionMapper.updateById(update);
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(String sessionId, ChatMessageQueryDTO queryDTO) {
        checkSessionOwner(sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        if (queryDTO.getLimit() != null) {
            wrapper.last("LIMIT " + queryDTO.getLimit());
        }
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        return messages.stream().map(msg -> {
            ChatMessageVO vo = new ChatMessageVO();
            BeanUtil.copyProperties(msg, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SseEmitter sendMsg(ChatSendDTO dto) {
        String userId = getCurrentUserId();
        String sessionId = dto.getSessionId();

        // 1. 处理会话
        boolean isNewSession = false;
        if (StrUtil.isBlank(sessionId)) {
            ChatSessionCreateDTO createDTO = new ChatSessionCreateDTO();
            createDTO.setTitle(StrUtil.sub(dto.getContent(), 0, 10));
            sessionId = createSession(createDTO);
            isNewSession = true;
        } else {
            checkSessionOwner(sessionId);
        }

        // 2. 准备上下文 (Redis 优先策略)
        List<Map<String, String>> historyContext = getContext(sessionId);

        // 3. 落库用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContext(dto.getContent());
        userMsg.setPhoto(dto.getPhoto());
        userMsg.setFile(dto.getFile());
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) userMsg.setCategory("text_photo");
        else if (dto.getFile() != null && !dto.getFile().isEmpty()) userMsg.setCategory("text_file");
        else userMsg.setCategory("text");

        chatMessageMapper.insert(userMsg);

        // 将用户新消息同步写入 Redis 上下文
        appendContext(sessionId, "user", dto.getContent());

        // 4. 预保存 AI 回复 (占位)
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("assistant");
        aiMsg.setContext("");
        aiMsg.setCategory("text");
        chatMessageMapper.insert(aiMsg);

        String aiMsgId = aiMsg.getId();
        String finalSessionId = sessionId;
        boolean finalIsNewSession = isNewSession;

        // 5. 建立 SSE 连接
        SseEmitter emitter = new SseEmitter(0L);

        // 6. 异步执行流式交互
        sseExecutor.execute(() -> {
            try {
                // A. 发送首帧
                if (finalIsNewSession) {
                    sendSseChunk(emitter, finalSessionId, aiMsgId, "");
                }

                StringBuilder fullResponse = new StringBuilder();

                // B. 调用封装好的 Client
                aiModelClient.streamChat(dto.getContent(), historyContext)
                        .doOnNext(token -> {
//                            log.info("【探针】Java收到Python Token: {}", token);
                            // --- 收到一个字：推给前端 ---
                            sendSseChunk(emitter, finalSessionId, aiMsgId, token);
                            fullResponse.append(token);
                        })
                        .doOnError(e -> {
                            // --- 发生错误 ---
                            log.error("AI 服务调用异常, SessionId: {}", finalSessionId, e);
                            emitter.completeWithError(e);
                        })
                        .doOnComplete(() -> {
                            // --- 流结束：收尾工作 ---
                            String finalContent = fullResponse.toString();

                            // 1. 发送结束标志
                            sendSseEnd(emitter, finalSessionId, aiMsgId);

                            // 2. 更新 DB (完整回复)
                            ChatMessage updateMsg = new ChatMessage();
                            updateMsg.setId(aiMsgId);
                            updateMsg.setContext(finalContent);
                            chatMessageMapper.updateById(updateMsg);

                            // 3. 更新 Redis (追加上下文)
                            appendContext(finalSessionId, "assistant", finalContent);

                            // 4. 关闭连接
                            emitter.complete();
                        })
                        .subscribe(); // 触发订阅

            } catch (Exception e) {
                log.error("SSE 线程启动失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void checkSessionOwner(String sessionId) {
        String userId = getCurrentUserId();
        ChatSession session = chatSessionMapper.selectById(sessionId);

        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (session.getStatus() == 2) {
            throw new BusinessException("会话已被删除");
        }
        if (!session.getUserId().equals(userId)) {
            log.warn("越权访问警告: User {} 尝试访问 Session {}", userId, sessionId);
            throw new BusinessException("无权访问该会话");
        }
    }

    /**
     * 发送 SSE 数据块
     */
    private void sendSseChunk(SseEmitter emitter, String sessionId, String msgId, String text) {
        try {
            emitter.send(ChatStreamVO.chunk(sessionId, msgId, text));
        } catch (IOException e) {
            log.warn("SSE 发送 Chunk 失败: {}", e.getMessage());
        }
    }

    /**
     * 发送 SSE 结束标志
     */
    private void sendSseEnd(SseEmitter emitter, String sessionId, String msgId) {
        try {
            emitter.send(ChatStreamVO.end(sessionId, msgId));
        } catch (IOException e) {
            log.warn("SSE 发送 End 失败: {}", e.getMessage());
        }
    }

    /**
     * 获取上下文 (Cache-Aside)
     */
    private List<Map<String, String>> getContext(String sessionId) {
        String cacheKey = "chat:context:" + sessionId;

        List<Object> cachedList = redisUtil.lGet(cacheKey, 0, -1);
        List<Map<String, String>> context = new ArrayList<>();

        if (cachedList != null && !cachedList.isEmpty()) {
            for (Object obj : cachedList) {
                if (obj instanceof Map) {
                    context.add((Map<String, String>) obj);
                }
            }
            redisUtil.expire(cacheKey, CONTEXT_TTL);
            return context;
        }

        // Cache Miss: 从 DB 加载
        log.info("Redis上下文缺失，从DB加载: {}", sessionId);
        List<ChatMessage> dbMsgs = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT " + MAX_CONTEXT_COUNT)
        );

        Collections.reverse(dbMsgs);

        for (ChatMessage msg : dbMsgs) {
            Map<String, String> item = new HashMap<>();
            item.put("role", msg.getRole());
            item.put("content", msg.getContext());
            context.add(item);
            redisUtil.lSet(cacheKey, item);
        }

        if (!context.isEmpty()) {
            redisUtil.expire(cacheKey, CONTEXT_TTL);
        }
        return context;
    }

    private void appendContext(String sessionId, String role, String content) {
        String cacheKey = "chat:context:" + sessionId;
        Map<String, String> item = new HashMap<>();
        item.put("role", role);
        item.put("content", content);
        redisUtil.lSet(cacheKey, item);
        redisUtil.lTrim(cacheKey, MAX_CONTEXT_COUNT);
        redisUtil.expire(cacheKey, CONTEXT_TTL);
    }
}