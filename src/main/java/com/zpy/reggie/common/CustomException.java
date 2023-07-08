package com.zpy.reggie.common;

/**
 * 自定义异常类
 */
public class CustomException extends RuntimeException{
    public CustomException(String msg) {
        super(msg);
    }
}
