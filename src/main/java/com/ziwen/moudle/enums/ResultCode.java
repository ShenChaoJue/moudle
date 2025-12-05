package com.ziwen.moudle.enums;

/**
 * 响应状态码枚举
 * 定义系统中常用的状态码和对应的提示信息
 *
 * @author AnYuan
 * @since 2024
 */
public enum ResultCode {

    // ========== 成功相关 ==========
    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    // ========== 客户端错误 4xx ==========
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    /**
     * 请求参数缺失
     */
    PARAM_MISSING(400, "请求参数缺失"),
    /**
     * 参数格式错误
     */
    PARAM_FORMAT_ERROR(400, "参数格式错误"),
    /**
     * 参数校验失败
     */
    PARAM_VALID_ERROR(400, "参数校验失败"),
    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),
    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),
    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    // ========== 业务错误 4xx ==========
    /**
     * 业务处理失败
     */
    BUSINESS_ERROR(400, "业务处理失败"),
    /**
     * 数据已存在
     */
    DATA_EXISTS(400, "数据已存在"),
    /**
     * 数据不存在
     */
    DATA_NOT_EXISTS(400, "数据不存在"),
    /**
     * 操作频繁，请稍后再试
     */
    TOO_MANY_REQUESTS(429, "操作频繁，请稍后再试"),

    // ========== 服务器错误 5xx ==========
    /**
     * 系统内部错误
     */
    SYSTEM_ERROR(500, "系统内部错误"),
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    /**
     * 网关错误
     */
    GATEWAY_ERROR(504, "网关错误"),

    // ========== 支付相关 6xx ==========
    /**
     * 支付失败
     */
    PAYMENT_FAILED(600, "支付失败"),
    /**
     * 支付超时
     */
    PAYMENT_TIMEOUT(601, "支付超时"),
    /**
     * 支付取消
     */
    PAYMENT_CANCEL(602, "支付取消"),
    /**
     * 退款失败
     */
    REFUND_FAILED(603, "退款失败"),
    /**
     * 订单不存在
     */
    ORDER_NOT_EXISTS(604, "订单不存在"),
    /**
     * 订单已支付
     */
    ORDER_PAID(605, "订单已支付"),
    /**
     * 订单未支付
     */
    ORDER_NOT_PAID(606, "订单未支付");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 提示信息
     */
    private final String message;

    /**
     * 构造函数
     *
     * @param code    状态码
     * @param message 提示信息
     */
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取提示信息
     *
     * @return 提示信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return ResultCode
     */
    public static ResultCode getByCode(Integer code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return SYSTEM_ERROR;
    }

    @Override
    public String toString() {
        return "ResultCode{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}