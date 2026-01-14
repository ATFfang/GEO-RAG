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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@TableName(value = "chat_session", autoResultMap = true)
public class ChatSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Session UUID (必填)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 对应用户的id字段 (必填)
     */
    private Long userId;

    /**
     * Session名称 (必填)
     */
    private String title;

    /**
     * 配置信息 (选填)
     * 使用项目中定义的 JSONB 处理器
     */
    @TableField(typeHandler = MybatisJsonTypeHandler.class)
    private Map<String, Object> metaInfo;

    /**
     * 1-正常, 2-归档/删除 (必填)
     */
    private Integer status;

    /**
     * 创建时间 (自动填充)
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间 (自动填充)
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
