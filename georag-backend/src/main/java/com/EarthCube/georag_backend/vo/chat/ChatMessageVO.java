package com.EarthCube.georag_backend.vo.chat;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ChatMessageVO {

    private String id;

    /**
     * 角色: "user" 或 "assistant"
     */
    private String role;

    /**
     * 消息内容 (Markdown 格式)
     */
    private String context;

    /**
     * 消息分类: text, text_file, text_photo
     */
    private String category;

    private List<String> photo;

    private List<Map<String, Object>> file;

    private LocalDateTime createTime;
}