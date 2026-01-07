package com.EarthCube.georag_backend.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UserProfileVO {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;

    // 返回给前端的状态码 (0/1/2)
    private Integer gender;
    // 返回给前端的中文描述 (男/女)
    private String genderDesc;

    private String region;

    private Integer status;
    private String statusDesc;

    // 格式化时间，符合文档示例 "2026-01-06 10:00:00"
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private Map<String, Object> settings;
}
