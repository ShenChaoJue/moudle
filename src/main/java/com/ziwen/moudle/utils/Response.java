package com.ziwen.moudle.utils;

import java.io.Serializable;

import com.ziwen.moudle.enums.ResultCode;

/**
 * 统一响应结果封装类
 * 提供成功的响应、失败的响应等多种响应方式
 *
 * @author AnYuan
 * @since 2024
 */
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 私有构造函数
     */
    private Response() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 私有构造函数
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     */
    private Response(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功响应方法 ====================

    /**
     * 成功响应（无数据）
     *
     * @param <T> 泛型
     * @return Response
     */
    public static <T> Response<T> success() {
        return new Response<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 数据
     * @param <T>  泛型
     * @return Response
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 消息
     * @param <T>     泛型
     * @return Response
     */
    public static <T> Response<T> success(String message) {
        return new Response<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    /**
     * 成功响应（自定义消息和数据）
     *
     * @param message 消息
     * @param data    数据
     * @param <T>     泛型
     * @return Response
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 成功响应（自定义状态码和消息）
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @param <T>     泛型
     * @return Response
     */
    public static <T> Response<T> success(Integer code, String message, T data) {
        return new Response<>(code, message, data);
    }

    // ==================== 失败响应方法 ====================

    /**
     * 失败响应（默认系统异常）
     *
     * @param <T> 泛型
     * @return Response
     */
    public static <T> Response<T> fail() {
        return new Response<>(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     *
     * @param message 消息
     * @param <T>     泛型
     * @return Response
     */
    public static <T> Response<T> fail(String message) {
        return new Response<>(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }

    /**
     * 失败响应（自定义状态码和消息）
     *
     * @param code    状态码
     * @param message 消息
     * @param <T>     泛型
     * @return Response
     */
    public static <T> Response<T> fail(Integer code, String message) {
        return new Response<>(code, message, null);
    }

    /**
     * 失败响应（根据ResultCode枚举）
     *
     * @param resultCode 结果码枚举
     * @param <T>        泛型
     * @return Response
     */
    public static <T> Response<T> fail(ResultCode resultCode) {
        return new Response<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（根据ResultCode枚举和数据）
     *
     * @param resultCode 结果码枚举
     * @param data       数据
     * @param <T>        泛型
     * @return Response
     */
    public static <T> Response<T> fail(ResultCode resultCode, T data) {
        return new Response<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    // ==================== Getter和Setter ====================

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    // ==================== 判断方法 ====================

    /**
     * 判断是否成功
     *
     * @return boolean
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 判断是否失败
     *
     * @return boolean
     */
    public boolean isFail() {
        return !isSuccess();
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}