package com.EarthCube.georag_backend.dto.chat;

import lombok.Data;

@Data
public class ChatSessionQueryDTO {

    // 当前页码，默认1
    private Integer current = 1;

    // 每页条数，默认20
    private Integer size = 20;

    // 搜索关键词 (匹配标题)
    private String keyword;
}