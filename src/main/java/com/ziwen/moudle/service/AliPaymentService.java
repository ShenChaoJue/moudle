package com.ziwen.moudle.service;

import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.ziwen.moudle.config.AliPayConfig;
import com.ziwen.moudle.constant.PayConstant;
import com.ziwen.moudle.dto.Order;
import com.ziwen.moudle.utils.Response;
import com.ziwen.moudle.enums.TradeTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author AnYuan
 * @description 支付宝原生支付服务
 */

@Service
@Slf4j
public class AliPaymentService implements PaymentService {
    
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AliPayConfig alipayConfig;
    
    @Override
    public <T> Response<T> pay(Order order) {

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

        AlipayTradePagePayModel pagePayModel = new AlipayTradePagePayModel();
        pagePayModel.setOutTradeNo(order.getOrderNo());
        pagePayModel.setSubject(order.getSubject());
        pagePayModel.setTotalAmount(order.getAmount().toString());

        //销售产品码，与支付宝签约的产品码名称。注：目前电脑支付场景下仅支持FAST_INSTANT_TRADE_PAY
        pagePayModel.setProductCode(TradeTypeEnum.getByName(order.getTradeType()).getAliTradeType());
        // 订单超时时间
        pagePayModel.setTimeoutExpress(PayConstant.ALI_TIMEOUT_EXPRESS);

        if (TradeTypeEnum.PC.name().equals(order.getTradeType())) {
            //PC扫码支付的方式:订单码-可定义宽度的嵌入式二维码
            pagePayModel.setQrPayMode(PayConstant.ALI_QR_PAY_MODE);
            // 商户自定义二维码宽度
//        pagePayModel.setQrcodeWidth(PayConstant.AliPayConstants.ALIPAY_PC_QR_WIDTH);
        }

        alipayRequest.setBizModel(pagePayModel);
        try {

            AlipayResponse response;
            if (TradeTypeEnum.PC.name().equals(order.getTradeType())) {

                log.info("==================== 支付宝支付请求开始 ====================");
                log.info("支付宝支付请求参数：{}", JSONObject.toJSONString(alipayRequest));
                log.info("支付宝网关地址：{}", alipayConfig.getServerUrl());
                log.info("支付宝应用ID：{}", alipayConfig.getAppId());

                response = alipayClient.pageExecute(alipayRequest);

                if (response == null) {
                    log.error("支付宝响应为空");
                    return Response.fail("支付宝响应为空");
                }

                log.info("支付宝支付请求结果：{}", JSONObject.toJSONString(response));
                log.info("支付宝响应状态码：{}", response.getCode());
                log.info("支付宝响应消息：{}", response.getMsg());
                log.info("支付宝响应子码：{}", response.getSubCode());
                log.info("支付宝响应子消息：{}", response.getSubMsg());
                log.info("==================== 支付宝支付请求结束 ====================");

                @SuppressWarnings("unchecked")
                Response<T> result = (Response<T>) Response.success(response);
                return result;

            } else if (TradeTypeEnum.APP.name().equals(order.getTradeType())) {

                log.info("==================== 支付宝APP支付请求开始 ====================");
                log.info("支付宝APP支付请求参数：{}", JSONObject.toJSONString(alipayRequest));

                response = alipayClient.sdkExecute(alipayRequest);

                if (response == null) {
                    log.error("支付宝APP支付响应为空");
                    return Response.fail("支付宝APP支付响应为空");
                }

                log.info("支付宝APP支付响应：{}", JSONObject.toJSONString(response));

                @SuppressWarnings("unchecked")
                Response<T> result = (Response<T>) Response.success(response);
                log.info("==================== 支付宝APP支付请求结束 ====================");
                return result;

            } else {
                return Response.fail("不支持的支付类型");
            }
        }catch (Exception e) {
            log.info("支付宝请求支付失败:{}", e.getMessage());
            return Response.fail("支付宝请求支付失败");
        }
    }
    
    @Override
    public <T> Response<T> query(String orderNo) {
        log.info("==================== 支付宝订单查询开始 ====================");
        log.info("查询订单号：{}", orderNo);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toJSONString());

        try {
            log.info("支付宝查询请求参数：{}", JSONObject.toJSONString(request));
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response == null) {
                log.error("支付宝查询响应为空");
                return Response.fail("支付宝查询响应为空");
            }

            log.info("支付宝查询响应：{}", JSONObject.toJSONString(response));
            log.info("查询响应状态码：{}", response.getCode());
            log.info("查询响应消息：{}", response.getMsg());
            log.info("查询响应子码：{}", response.getSubCode());
            log.info("查询响应子消息：{}", response.getSubMsg());

            @SuppressWarnings("unchecked")
            Response<T> result = (Response<T>) Response.success(response);
            log.info("==================== 支付宝订单查询结束 ====================");
            return result;
        } catch (Exception e) {
            log.error("支付宝请求查询失败", e);
            return Response.fail("支付宝请求查询失败: " + e.getMessage());
        }
    }
    
    @Override
    public <T> Response<T> refund(Order order) {
        log.info("==================== 支付宝退款请求开始 ====================");
        log.info("退款订单号：{}", order.getOrderNo());
        log.info("退款金额：{}", order.getAmount());

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("refund_amount", order.getAmount());

        request.setReturnUrl(alipayConfig.getReturnUrl());
        request.setBizContent(bizContent.toJSONString());

        try {
            log.info("支付宝退款请求参数：{}", JSONObject.toJSONString(request));
            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response == null) {
                log.error("支付宝退款响应为空");
                return Response.fail("支付宝退款响应为空");
            }

            log.info("支付宝退款响应：{}", JSONObject.toJSONString(response));
            log.info("退款响应状态码：{}", response.getCode());
            log.info("退款响应消息：{}", response.getMsg());
            log.info("退款响应子码：{}", response.getSubCode());
            log.info("退款响应子消息：{}", response.getSubMsg());

            @SuppressWarnings("unchecked")
            Response<T> result = (Response<T>) Response.success(response);
            log.info("==================== 支付宝退款请求结束 ====================");
            return result;
        } catch (Exception e) {
            log.error("支付宝请求退款失败", e);
            return Response.fail("支付宝请求退款失败: " + e.getMessage());
        }
    }
    
    @Override
    public void handleNotify(HttpServletRequest httpServletRequest) throws Exception {
        log.info("==================== 支付宝回调通知开始 ====================");

        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = httpServletRequest.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = httpServletRequest.getParameter(paramName);
            params.put(paramName, paramValue);
        }

        log.info("支付宝回调通知参数：{}", JSONObject.toJSONString(params));

        try {
            boolean signVerified = AlipaySignature.rsaCheckV2(params, alipayConfig.getPublicKey(), PayConstant.UTF_8, PayConstant.SIGN_TYPE_RSA2);

            if (!signVerified) {
                log.error("支付宝回调通知签名验证失败");
                log.error("回调参数：{}", JSONObject.toJSONString(params));
                throw new Exception("签名验证失败");
            }

            log.info("支付宝回调通知签名验证成功");

            // 商户订单号
            String orderNO = params.get("out_trade_no");
            // 支付宝交易号
            String tradeNo = params.get("trade_no");
            // 支付状态
            String tradeStatus = params.get("trade_status");
            // 交易金额
            String totalAmount = params.get("total_amount");

            log.info("订单号：{}", orderNO);
            log.info("支付宝交易号：{}", tradeNo);
            log.info("支付状态：{}", tradeStatus);
            log.info("交易金额：{}", totalAmount);

            // TODO: 在这里处理订单状态更新逻辑
            // 例如：根据 tradeStatus 更新订单状态

            log.info("==================== 支付宝回调通知处理成功 ====================");

        } catch (Exception e) {
            log.error("支付宝回调通知失败:{}", e.getMessage(), e);
            throw new Exception("支付宝回调通知处理失败: " + e.getMessage());
        }
    }
}
