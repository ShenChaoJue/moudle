package com.ziwen.moudle.service.pay;

import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.dto.pay.Order;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付服务接口
 * 定义统一的支付相关操作方法
 *
 * @author AnYuan
 * @since 2024
 */
public interface PaymentService {

    /**
     * 创建支付订单
     * 根据订单信息调用对应的支付服务，创建支付订单并返回支付二维码或跳转链接
     *
     * @param order 订单信息
     * @return AjaxResult 支付结果
     */
    AjaxResult pay(Order order);

    /**
     * 查询订单支付状态
     * 根据订单号查询支付结果
     *
     * @param orderNo 订单号
     * @return AjaxResult 支付状态信息
     */
    AjaxResult query(String orderNo);

    /**
     * 申请退款
     * 根据订单信息调用退款接口
     *
     * @param order 退款订单信息
     * @return AjaxResult 退款结果
     */
    AjaxResult refund(Order order);

    /**
     * 处理支付回调
     * 处理第三方支付平台的异步回调通知
     *
     * @param request HttpServletRequest
     * @throws Exception 处理过程中的异常
     */
    void handleNotify(HttpServletRequest request) throws Exception;
}