package com.EarthCube.georag_backend.vo.chat;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSessionVO {

    private String id;

    private String title;

    // 用于前端显示 "刚刚"、"1小时前"
    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}