package com.EarthCube.georag_backend.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户账号状态枚举
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    BANNED(0, "禁用"),
    NORMAL(1, "正常"),
    INACTIVE(2, "未激活"),
    DELETED(3, "注销");

    @EnumValue
    private final int code;

    private final String desc;

    @JsonValue
    public int getCode() {
        return code;
    }
}
