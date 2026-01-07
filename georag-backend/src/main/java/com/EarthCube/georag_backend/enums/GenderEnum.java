package com.EarthCube.georag_backend.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户性别枚举
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {
    SECRET(0, "保密"),
    MALE(1, "男"),
    FEMALE(2, "女");

    /**
     * 存入数据库的值 (0, 1, 2)
     */
    @EnumValue
    private final int code;

    /**
     * 前端显示的描述
     */
    private final String desc;

    // @JsonValue 保证序列化给前端时返回的是 code (0/1/2) 而不是 "MALE"
    // 如果希望返回 "男"，则加在 desc 上，但通常 API 交互用状态码
    @JsonValue
    public int getCode() {
        return code;
    }
}
