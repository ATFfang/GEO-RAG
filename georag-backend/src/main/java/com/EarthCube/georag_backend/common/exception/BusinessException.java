package com.EarthCube.georag_backend.common.exception;

import lombok.Getter;

/**
 * 业务逻辑异常（如：用户名已存在、余额不足等）
 */
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500; // 默认系统错误
    }
}