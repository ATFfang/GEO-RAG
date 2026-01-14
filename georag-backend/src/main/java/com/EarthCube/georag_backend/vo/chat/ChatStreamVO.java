package com.EarthCube.georag_backend.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamVO {

    /**
     * 会话ID (通常只在第一帧返回，用于前端更新 URL)
     */
    private String sessionId;

    /**
     * 当前正在生成的消息ID (用于后续的点赞、引用操作)
     */
    private String messageId;

    /**
     * 增量文本 (Delta Text)
     */
    private String text;

    /**
     * 是否结束标志
     */
    private Boolean finish;

    // --- 辅助静态方法，方便 Service 层调用 ---

    public static ChatStreamVO chunk(String sessionId, String msgId, String text) {
        return new ChatStreamVO(sessionId, msgId, text, false);
    }

    public static ChatStreamVO end(String sessionId, String msgId) {
        return new ChatStreamVO(sessionId, msgId, "", true);
    }
}