package com.EarthCube.georag_backend.common.exception;

import lombok.Getter;

/**
 * 参数校验异常（如：邮箱格式错误、必填项缺失）
 */
@Getter
public class ValidateException extends RuntimeException {
    private final Integer code;

    public ValidateException(String message) {
        super(message);
        this.code = 400; // 默认参数错误状态码
    }

    public ValidateException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
