package com.EarthCube.georag_backend.dto.chat;

import lombok.Data;

@Data
public class ChatMessageQueryDTO {

    /**
     * 游标：上一页最后一条消息的 ID
     * 第一页传 null
     */
    private String cursor;

    /**
     * 加载条数
     */
    private Integer limit = 20;
}
