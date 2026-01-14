package com.EarthCube.georag_backend.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class ChatSessionCreateDTO {

    @Size(max = 50, message = "标题长度不能超过50")
    private String title;

    /**
     * 初始配置信息
     */
    private Map<String, Object> metaInfo;
}
