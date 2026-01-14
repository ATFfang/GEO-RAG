package com.EarthCube.georag_backend.service;

import com.EarthCube.georag_backend.dto.chat.*;
import com.EarthCube.georag_backend.vo.chat.ChatMessageVO;
import com.EarthCube.georag_backend.vo.chat.ChatSessionVO;
import com.EarthCube.georag_backend.common.result.PageResult; // 假设你有通用的分页返回类，如果没有可以用 IPage<ChatSessionVO>
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface IChatService {

    /**
     * 创建新会话
     */
    String createSession(ChatSessionCreateDTO dto);

    /**
     * 分页查询会话列表
     */
    Page<ChatSessionVO> getSessionList(ChatSessionQueryDTO queryDTO);

    /**
     * 修改会话标题
     */
    void updateSession(String sessionId, ChatSessionUpdateDTO dto);

    /**
     * 删除会话
     */
    void deleteSession(String sessionId);

    /**
     * 获取会话历史消息 (游标分页)
     */
    List<ChatMessageVO> getSessionMessages(String sessionId, ChatMessageQueryDTO queryDTO);

    /**
     * 发送消息并获取流式响应 (核心)
     */
    SseEmitter sendMsg(ChatSendDTO dto);
}