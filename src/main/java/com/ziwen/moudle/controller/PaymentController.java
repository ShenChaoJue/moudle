package com.ziwen.moudle.controller;

import com.ziwen.moudle.dto.Order;
import com.ziwen.moudle.enums.PaymentTypeEnum;
import com.ziwen.moudle.factory.PaymentFactory;
import com.ziwen.moudle.service.AliPaymentService;
import com.ziwen.moudle.service.PaymentService;
import com.ziwen.moudle.service.WxPaymentService;
import com.ziwen.moudle.utils.Response;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付控制器
 * 提供统一支付接口，支持微信支付和支付宝支付
 * 包含支付、退款、查询及回调处理功能
 *
 * @author AnYuan
 * @since 2024
 */

@RestController
@RequestMapping("/api")
@Tag(name = "支付管理", description = "统一支付接口，支持微信和支付宝")
public class PaymentController {
    
    @Autowired
    private PaymentFactory paymentFactory;
    @Autowired
    private WxPaymentService wxPaymentService;
    @Autowired
    private AliPaymentService aliPaymentService;

    /**
     * 创建支付订单
     * 根据支付类型（微信/支付宝）调用对应的支付服务
     *
     * @param order 订单信息，包含订单号、金额、支付类型等
     * @return Response 支付结果响应，包含支付二维码/跳转信息
     */
    @PostMapping("/pay")
    @Operation(
        summary = "创建支付订单",
        description = "根据订单信息调用微信或支付宝支付，返回支付二维码或跳转链接"
    )
    @ApiResponse(responseCode = "200", description = "支付成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    @ApiResponse(responseCode = "500", description = "支付服务异常")
    public Response<?> pay(
            @Parameter(description = "订单信息", required = true) @RequestBody Order order){

        // 校验订单参数
        paymentFactory.checkOrder(order);

        PaymentService paymentService = paymentFactory.getPaymentService(PaymentTypeEnum.getByName(order.getPaymentType()));
        return paymentService.pay(order);
    }

    /**
     * 查询订单支付状态
     * 根据订单号和支付类型查询支付结果
     *
     * @param orderNo 订单号
     * @param paymentType 支付类型：WX_PAY（微信）、ALI_PAY（支付宝）
     * @return Response 订单支付状态信息
     */
    @GetMapping("/query/{orderNo}/{paymentType}")
    @Operation(
        summary = "查询订单支付状态",
        description = "根据订单号查询微信或支付宝订单的支付状态和详情"
    )
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    @ApiResponse(responseCode = "404", description = "订单不存在")
    public Response<?> query(
            @Parameter(description = "订单号", required = true) @PathVariable("orderNo") String orderNo,
            @Parameter(description = "支付类型：WX_PAY-微信支付，ALI_PAY-支付宝支付", required = true) @PathVariable("paymentType") String paymentType) {
        PaymentService paymentService = paymentFactory.getPaymentService(PaymentTypeEnum.getByName(paymentType));
        return paymentService.query(orderNo);
    }

    /**
     * 申请退款
     * 根据订单信息调用微信或支付宝退款接口
     *
     * @param order 退款订单信息，包含原订单号、退款金额、退款原因等
     * @return Response 退款结果响应
     */
    @PostMapping("/refund")
    @Operation(
        summary = "申请退款",
        description = "根据订单信息调用微信或支付宝退款接口，处理退款申请"
    )
    @ApiResponse(responseCode = "200", description = "退款申请成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    @ApiResponse(responseCode = "500", description = "退款服务异常")
    public Response<?> refund(
            @Parameter(description = "退款订单信息", required = true) @RequestBody Order order) {
        PaymentService paymentService = paymentFactory.getPaymentService(PaymentTypeEnum.getByName(order.getPaymentType()));
        return paymentService.refund(order);
    }

    /**
     * 微信支付异步回调
     * 接收微信支付平台的异步通知，处理支付结果更新
     * 注意：此接口由微信支付平台调用，不需要前端发起请求
     *
     * @param request HttpServletRequest，包含微信回调的XML数据
     * @return String 回调处理结果：success-成功，fail-失败
     */
    @PostMapping("/notify/wx")
    @Operation(
        summary = "微信支付异步回调",
        description = "微信支付平台异步通知接口，处理支付成功后的业务逻辑"
    )
    @ApiResponse(responseCode = "200", description = "回调处理成功")
    public String wxHandleNotify(
            @Parameter(description = "微信回调请求", hidden = true) HttpServletRequest request) {
        try {
            wxPaymentService.handleNotify(request);
            return WxPayNotifyResponse.success("回调处理成功");
        } catch (Exception e) {
            return WxPayNotifyResponse.failResp("回调处理失败");
        }
    }
    
    /**
     * 支付宝异步回调
     * 接收支付宝支付平台的异步通知，处理支付结果更新
     * 注意：此接口由支付宝支付平台调用，不需要前端发起请求
     *
     * @param request HttpServletRequest，包含支付宝回调的参数数据
     * @return String 回调处理结果：success-成功，failure-失败
     */
    @PostMapping("/notify/ali")
    @Operation(
        summary = "支付宝异步回调",
        description = "支付宝支付平台异步通知接口，处理支付成功后的业务逻辑"
    )
    @ApiResponse(responseCode = "200", description = "回调处理成功")
    public String aliHandleNotify(HttpServletRequest request) {
        try {
            aliPaymentService.handleNotify(request);
            return "success";
        } catch (Exception e) {
            return "failure";
        }
    }
}
