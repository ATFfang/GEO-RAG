package com.EarthCube.georag_backend.controller;

import com.EarthCube.georag_backend.common.exception.BusinessException;
import com.EarthCube.georag_backend.common.exception.ValidateException;
import com.EarthCube.georag_backend.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(ValidateException.class)
    public Result<?> handleValidateException(ValidateException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理系统未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统未知异常", e);
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}