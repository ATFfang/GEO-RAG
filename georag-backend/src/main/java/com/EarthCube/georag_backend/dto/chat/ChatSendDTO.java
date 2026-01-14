package com.EarthCube.georag_backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatSendDTO {

    /**
     * 会话ID
     * 若为空，后端将自动创建一个新会话
     */
    private String sessionId;

    /**
     * 用户提问内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容过长")
    private String content;

    /**
     * 引用图片列表 (MinIO URL)
     */
    private List<String> photo;

    /**
     * 引用文件列表 (包含元数据)
     */
    private List<Map<String, Object>> file;

    /**
     * 可选：模型参数配置 (如 model: "gpt-4")
     */
    private Map<String, Object> options;
}