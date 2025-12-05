package com.ziwen.moudle.factory;

import com.ziwen.moudle.dto.Order;
import com.ziwen.moudle.enums.PaymentTypeEnum;
import com.ziwen.moudle.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author AnYuan
 * @description 支付工厂
 */

@Service
public class PaymentFactory {
    @Autowired
    private  WxPaymentService wxPaymentService;
    @Autowired
    private AliPaymentService alipayService;
    
    public void checkOrder(Order order) {
        // 校验参数
    }
    
    public PaymentService getPaymentService(PaymentTypeEnum paymentTypeEnum) {
        if (paymentTypeEnum == null) {
            throw new IllegalArgumentException("不支持的支付类型");
        }
        switch (paymentTypeEnum) {
            case WXPAY:
                return wxPaymentService;
            case ALIPAY:
                return alipayService;
            default:
                throw new IllegalArgumentException("不支持的支付类型");
        }
    }
}