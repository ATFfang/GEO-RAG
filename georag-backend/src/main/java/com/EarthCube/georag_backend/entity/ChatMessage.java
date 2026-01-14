package com.EarthCube.georag_backend.entity;

import com.EarthCube.georag_backend.handler.MybatisJsonTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Message id (必填)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 对应会话的 session id (必填)
     */
    private String sessionId;

    /**
     * 角色: "user", "assistant" (必填)
     */
    private String role;

    /**
     * 消息文本内容 (必填)
     */
    @TableField("context")
    private String context;

    /**
     * 消息分类 (自动)
     * 建议在 Service 层根据 photo 和 file 是否为空自动赋值：
     * "text", "text_file", "text_photo"
     */
    private String category;

    /**
     * 图片列表 (JSONB, 必填)
     * 存储 MinIO 经过序列化的图片 URL 数组
     */
    @TableField(typeHandler = MybatisJsonTypeHandler.class)
    private List<String> photo;

    /**
     * 文件列表 (JSONB, 必填)
     * 存储包含 MinIO 链接及元数据的 JSON 对象数组
     */
    @TableField(typeHandler = MybatisJsonTypeHandler.class)
    private List<Map<String, Object>> file;

    /**
     * 创建时间 (自动填充)
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
