package com.EarthCube.georag_backend.controller;

import com.EarthCube.georag_backend.common.result.PageResult;
import com.EarthCube.georag_backend.common.result.Result;
import com.EarthCube.georag_backend.dto.chat.*;
import com.EarthCube.georag_backend.service.IChatService;
import com.EarthCube.georag_backend.vo.chat.ChatMessageVO;
import com.EarthCube.georag_backend.vo.chat.ChatSessionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天模块控制器
 * 包含会话管理和流式对话接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Autowired
    private IChatService chatService;

    /**
     * 1. 查询会话列表
     * GET /api/v1/chat/sessions?current=1&size=20&keyword=...
     */
    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionVO>> getSessionList(ChatSessionQueryDTO queryDTO) {
        // Service 返回 MyBatis Plus 的 Page 对象
        Page<ChatSessionVO> mpPage = chatService.getSessionList(queryDTO);
        // 转换为通用的 PageResult 返回
        return Result.success(PageResult.of(mpPage));
    }

    /**
     * 2. 创建新会话
     * POST /api/v1/chat/sessions
     */
    @PostMapping("/sessions")
    public Result<String> createSession(@RequestBody @Validated ChatSessionCreateDTO dto) {
        // userId 在 AuthInterceptor 中已注入上下文，Service 层会自动获取
        String sessionId = chatService.createSession(dto);
        return Result.success("会话创建成功", sessionId);
    }

    /**
     * 3. 发送消息并获取流式响应
     * POST /api/v1/chat/completions
     * 返回 text/event-stream 格式
     */
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMsg(@RequestBody @Validated ChatSendDTO dto) {
        // SSE 接口直接返回 Emitter，不要用 Result 包装
        return chatService.sendMsg(dto);
    }

    /**
     * 4. 查询会话历史消息 (支持游标分页)
     * GET /api/v1/chat/sessions/{sessionId}/messages?cursor=...&limit=20
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> getSessionMessages(
            @PathVariable String sessionId,
            ChatMessageQueryDTO queryDTO) {
        List<ChatMessageVO> list = chatService.getSessionMessages(sessionId, queryDTO);
        return Result.success(list);
    }

    /**
     * 5. 修改会话标题
     * PATCH /api/v1/chat/sessions/{sessionId}
     */
    @PatchMapping("/sessions/{sessionId}")
    public Result<Void> updateSession(
            @PathVariable String sessionId,
            @RequestBody @Validated ChatSessionUpdateDTO dto) {
        chatService.updateSession(sessionId, dto);
        return Result.success("标题修改成功", null);
    }

    /**
     * 6. 删除会话
     * DELETE /api/v1/chat/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success("会话删除成功", null);
    }
}