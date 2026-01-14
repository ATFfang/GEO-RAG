package com.EarthCube.georag_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.EarthCube.georag_backend.common.context.UserContext;
import com.EarthCube.georag_backend.common.exception.BusinessException;
import com.EarthCube.georag_backend.dto.chat.*;
import com.EarthCube.georag_backend.entity.ChatMessage;
import com.EarthCube.georag_backend.entity.ChatSession;
import com.EarthCube.georag_backend.mapper.ChatMessageMapper;
import com.EarthCube.georag_backend.mapper.ChatSessionMapper;
import com.EarthCube.georag_backend.service.IChatService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    // 假设你有一个 WebClient 或 OpenFeign 用于调用 Python 服务
    // @Autowired
    // private AiServiceClient aiServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 专门用于 SSE 推送的线程池，避免阻塞主线程
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * 获取当前登录用户 ID (Long类型)
     */
    private Long getCurrentUserId() {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new BusinessException("用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(ChatSessionCreateDTO dto) {
        Long userId = getCurrentUserId();

        ChatSession session = new ChatSession();
        // 使用 UUID (注意: 实体类里如果是 @TableId(type=ASSIGN_UUID) 则不用手动 set)
        // 但如果用 Hutool 手动控制也行
        // session.setId(IdUtil.simpleUUID());

        session.setUserId(userId);
        session.setTitle(StrUtil.isBlank(dto.getTitle()) ? "新对话" : dto.getTitle());
        session.setMetaInfo(dto.getMetaInfo());
        session.setStatus(1); // 正常

        chatSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public Page<ChatSessionVO> getSessionList(ChatSessionQueryDTO queryDTO) {
        Long userId = getCurrentUserId();

        Page<ChatSession> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();

        // 【关键鉴权】只查当前用户的
        wrapper.eq(ChatSession::getUserId, userId);
        wrapper.eq(ChatSession::getStatus, 1);

        // 搜索关键词
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            wrapper.like(ChatSession::getTitle, queryDTO.getKeyword());
        }

        // 按更新时间倒序
        wrapper.orderByDesc(ChatSession::getUpdateTime);

        chatSessionMapper.selectPage(page, wrapper);

        // 转换 VO
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
        // 1. 校验归属权
        checkSessionOwner(sessionId);

        // 2. 更新
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setTitle(dto.getTitle());
        chatSessionMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        checkSessionOwner(sessionId);

        // 逻辑删除
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setStatus(2); // 2-已删除
        chatSessionMapper.updateById(update);
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(String sessionId, ChatMessageQueryDTO queryDTO) {
        checkSessionOwner(sessionId);

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);

        // 游标分页逻辑 (create_time < cursor_msg_time OR id < cursor_id)
        // 简化版：这里演示普通根据时间正序加载，或者根据 cursor 向前加载
        // 实际高性能场景建议配合前端传最后一条消息的时间戳
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        if (queryDTO.getLimit() != null) {
            wrapper.last("LIMIT " + queryDTO.getLimit());
        }

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        return messages.stream().map(msg -> {
            ChatMessageVO vo = new ChatMessageVO();
            BeanUtil.copyProperties(msg, vo);
            // JSONB 字段如果在 Entity 里已经是 List/Map，BeanUtil 会自动拷贝
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SseEmitter sendMsg(ChatSendDTO dto) {
        Long userId = getCurrentUserId();
        String sessionId = dto.getSessionId();

        // 1. 处理会话 (如果为空则新建)
        boolean isNewSession = false;
        if (StrUtil.isBlank(sessionId)) {
            ChatSessionCreateDTO createDTO = new ChatSessionCreateDTO();
            createDTO.setTitle(StrUtil.sub(dto.getContent(), 0, 10)); // 默认取前10字做标题
            sessionId = createSession(createDTO);
            isNewSession = true;
        } else {
            checkSessionOwner(sessionId);
        }

        // 2. 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContext(dto.getContent());
        userMsg.setPhoto(dto.getPhoto());
        userMsg.setFile(dto.getFile());
        // 简单分类逻辑
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) userMsg.setCategory("text_photo");
        else if (dto.getFile() != null && !dto.getFile().isEmpty()) userMsg.setCategory("text_file");
        else userMsg.setCategory("text");

        chatMessageMapper.insert(userMsg);

        // 3. 预保存 AI 回复 (占位)
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("assistant");
        aiMsg.setContext(""); // 暂时为空
        aiMsg.setCategory("text");
        chatMessageMapper.insert(aiMsg);

        String aiMsgId = aiMsg.getId();
        String finalSessionId = sessionId;
        boolean finalIsNewSession = isNewSession;

        // 4. 建立 SSE 连接
        // timeout 设置为 0 表示永不超时 (实际建议设置 3-5 分钟)
        SseEmitter emitter = new SseEmitter(0L);

        // 5. 异步调用 Python AI 服务并推送流
        sseExecutor.execute(() -> {
            try {
                // A. 发送首帧 (包含 sessionId, messageId)
                if (finalIsNewSession) {
                    emitter.send(ChatStreamVO.chunk(finalSessionId, aiMsgId, ""));
                }

                StringBuilder fullResponse = new StringBuilder();

                // B. === 这里模拟 Python 服务的流式返回 ===
                // 实际开发中，这里应使用 WebClient 请求 Python 接口
                String mockAnswer = "这里是来自 AI 的模拟回复流式数据...";
                for (char c : mockAnswer.toCharArray()) {
                    // 模拟网络延迟
                    Thread.sleep(100);
                    // 推送 Chunk
                    emitter.send(ChatStreamVO.chunk(finalSessionId, aiMsgId, String.valueOf(c)));
                    fullResponse.append(c);
                }
                // ======================================

                // C. 推送结束帧
                emitter.send(ChatStreamVO.end(finalSessionId, aiMsgId));

                // D. 异步更新数据库中的 AI 回复内容
                ChatMessage updateMsg = new ChatMessage();
                updateMsg.setId(aiMsgId);
                updateMsg.setContext(fullResponse.toString());
                chatMessageMapper.updateById(updateMsg);

                // E. 结束 SSE
                emitter.complete();

            } catch (Exception e) {
                log.error("SSE 推送异常", e);
                emitter.completeWithError(e);

                // 也可以记录一条错误消息到数据库
            }
        });

        return emitter;
    }

    /**
     * 内部方法：校验会话归属权
     */
    private void checkSessionOwner(String sessionId) {
        Long userId = getCurrentUserId();
        ChatSession session = chatSessionMapper.selectById(sessionId);

        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (session.getStatus() == 2) { // 已删除
            throw new BusinessException("会话已被删除");
        }
        // 【核心安全校验】防止越权
        if (!session.getUserId().equals(userId)) {
            log.warn("越权访问警告: User {} 尝试访问 Session {}", userId, sessionId);
            throw new BusinessException("无权访问该会话");
        }
    }
}
