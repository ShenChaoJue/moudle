package com.ziwen.moudle.service.pay;

import com.alibaba.fastjson2.JSONObject;
import com.ziwen.moudle.common.AjaxResult;
import com.ziwen.moudle.config.WxPayProperties;
import com.ziwen.moudle.dto.pay.Order;
import com.ziwen.moudle.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.WxPayOrderQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

/**
 * @author AnYuan
 * @description 微信原生支付服务
 */

@Service
@Slf4j
public class WxPaymentService implements PaymentService {
    
    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private WxPayProperties properties;
    
    @Override
    public AjaxResult pay(Order order) {
        try {
            WxPayUnifiedOrderRequest request = WxPayUnifiedOrderRequest.newBuilder()
                .outTradeNo(order.getOrderNo())
                .productId(order.getOrderNo())
                .totalFee(order.getAmount().multiply(BigDecimal.valueOf(100)).intValue())
                .body(order.getSubject())
                .spbillCreateIp("127.0.0.1")
                .tradeType(TradeTypeEnum.getByName(order.getTradeType()).getWxTradeType())
                .notifyUrl(properties.getNotifyUrl())
                .build();

            log.info("微信支付请求参数：{}", JSONObject.toJSONString(request));
            WxPayUnifiedOrderResult result = wxPayService.unifiedOrder(request);
            log.info("微信支付请求结果：{}", JSONObject.toJSONString(request));

            result.setXmlString("");
            AjaxResult response = AjaxResult.success(result);
            return response;

        }catch ( Exception e) {
            log.info("微信请求支付失败:{}", e.getMessage());
            return AjaxResult.error("微信请求支付失败");
        }
    }
    
    @Override
    public AjaxResult query(String orderNo) {
        WxPayOrderQueryRequest request = new WxPayOrderQueryRequest();
        request.setOutTradeNo(orderNo);

        try {
            log.info("微信查询请求参数：{}", JSONObject.toJSONString(request));
            WxPayOrderQueryResult result = wxPayService.queryOrder(request);
            AjaxResult response = AjaxResult.success(result);
            return response;
        }catch ( Exception e){
            log.info("微信请求查询失败:{}", e.getMessage());
            return AjaxResult.error("微信请求查询失败");
        }
    }
    
    @Override
    public AjaxResult refund(Order order) {
        WxPayRefundRequest wxPayRefundRequest = new WxPayRefundRequest();
        wxPayRefundRequest.setOutTradeNo(order.getOrderNo());
        wxPayRefundRequest.setOutRefundNo(buildRefundNo(order.getOrderNo()));
        wxPayRefundRequest.setRefundFee(order.getAmount().multiply(new BigDecimal(100)).intValue());
        wxPayRefundRequest.setTotalFee(order.getAmount().multiply(new BigDecimal(100)).intValue());
        wxPayRefundRequest.setRefundDesc("退款");
        wxPayRefundRequest.setNotifyUrl(properties.getRefundNotifyUrl());

        try {
            log.info("微信查询请求参数：{}", JSONObject.toJSONString(wxPayRefundRequest));
            WxPayRefundResult refund = wxPayService.refund(wxPayRefundRequest);
            AjaxResult response = AjaxResult.success(refund);
            return response;
        } catch (WxPayException e) {
            log.info("微信请求退款失败:{}", e.getMessage());
            return AjaxResult.error("微信请求退款失败");
        }
    }
    
    @Override
    public void handleNotify(HttpServletRequest httpServletRequest) throws Exception {
        String xmlResult = IOUtils.toString(httpServletRequest.getInputStream(), httpServletRequest.getCharacterEncoding());

        // 转换xml格式数据为对象，并验证签名
        WxPayOrderNotifyResult notifyResult = wxPayService.parseOrderNotifyResult(xmlResult);

        // 商户订单号
        String outTradeNo = notifyResult.getOutTradeNo();

        // 处理订单状态
    }

    /**
     * 生成退款单号
     * 基于订单号加时间戳生成唯一的退款单号
     *
     * @param orderNo 原始订单号
     * @return 退款单号
     */
    private String buildRefundNo(String orderNo) {
        return orderNo + "_REF_" + System.currentTimeMillis();
    }
}
