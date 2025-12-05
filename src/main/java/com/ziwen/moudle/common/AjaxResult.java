package com.ziwen.moudle.common;

import java.util.HashMap;
import java.util.Objects;

import com.ziwen.moudle.constant.HttpStatus;

/**
 * 统一响应结果
 *
 * @author ziwen
 */
public class AjaxResult extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    /* 状态码 */
    public static final String CODE_TAG = "code";

    /* 返回内容 */
    public static final String MSG_TAG = "msg";

    /* 数据对象 */
    public static final String DATA_TAG = "data";

    // 空的构造方法
    public AjaxResult() {

    }

    /**
     * @description: 返回状态码 和 内容
     * @param: code 状态码 msg 内容
     * @return: json
     * @author: zi-wen write ruo-yi
     * @date: 2025/1/14
     */
    public AjaxResult(int code, String msg) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * @description: 返回状态码 内容 数据
     * @param: code mdg data
     * @return: json
     * @author: zi-wen
     * @date: 2025/1/14
     */

    public AjaxResult(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (Objects.nonNull(data)) {
            super.put(DATA_TAG, data);
        }
    }

    /** 返回操作成功消息 **/
    public static AjaxResult success() {
        return AjaxResult.success("操作成功");
    }

    /** 返回成功数据 **/
    public static AjaxResult success(Object data) {
        return AjaxResult.success("操作成功", data);
    }

    // 2.返回成功消息方法
    public static AjaxResult success(String msg) {
        return AjaxResult.success(msg, null);
    }

    // 1.构造返回成功方法
    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(HttpStatus.SUCCESS, msg, data);
    }

    // 1.构造警告方法
    public static AjaxResult warn(String msg, Object data) {
        return new AjaxResult(HttpStatus.BAD_REQUEST, msg, data);
    }

    // 返回警告内容
    public static AjaxResult warn(String msg) {
        return AjaxResult.warn(msg, null);
    }

    // 1.构造错误方法
    public static AjaxResult error(String msg, Object data) {
        return new AjaxResult(HttpStatus.ERROR, msg, data);
    }

    // 返回错误内容
    public static AjaxResult error(String msg) {
        return AjaxResult.error(msg, null);
    }

    // 返回操作失败消息
    public static AjaxResult error() {
        return AjaxResult.error("操作失败");
    }

    // 返回错误状态码 错误内容
    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    // 是否为成功消息
    public boolean isSuccess() {
        return Objects.equals(HttpStatus.SUCCESS, this.get(CODE_TAG));
    }

    // 是否为警告消息
    public boolean isWarn() {
        return Objects.equals(HttpStatus.BAD_REQUEST, this.get(CODE_TAG));
    }

    // 是否为错误消息
    public boolean isError() {
        return Objects.equals(HttpStatus.ERROR, this.get(CODE_TAG));
    }

    // 方便链式调用
    @Override
    public AjaxResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }

}
