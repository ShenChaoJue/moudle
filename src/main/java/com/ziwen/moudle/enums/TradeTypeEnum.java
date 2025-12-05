package com.ziwen.moudle.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易类型枚举
 * 定义支付场景类型（PC网站、APP等）
 *
 * @author AnYuan
 * @since 2024
 */
@Getter
@AllArgsConstructor
public enum TradeTypeEnum {

    /**
     * PC网站支付
     */
    PC("PC", "FAST_INSTANT_TRADE_PAY", "NATIVE"),

    /**
     * APP支付
     */
    APP("APP", "QUICK_MSECURITY_PAY", "APP"),

    /**
     * 手机网站支付
     */
    WAP("WAP", "QUICK_MSECURITY_PAY", "MWEB");

    /**
     * 交易类型编码
     */
    private final String code;

    /**
     * 支付宝交易类型
     */
    private final String aliTradeType;

    /**
     * 微信交易类型
     */
    private final String wxTradeType;

    /**
     * 根据名称获取枚举
     *
     * @param name 名称
     * @return TradeTypeEnum
     */
    public static TradeTypeEnum getByName(String name) {
        for (TradeTypeEnum tradeTypeEnum : values()) {
            if (tradeTypeEnum.name().equals(name)) {
                return tradeTypeEnum;
            }
        }
        return PC;
    }

    /**
     * 根据支付宝交易类型获取枚举
     *
     * @param aliTradeType 支付宝交易类型
     * @return TradeTypeEnum
     */
    public static TradeTypeEnum getByAliTradeType(String aliTradeType) {
        for (TradeTypeEnum tradeTypeEnum : values()) {
            if (tradeTypeEnum.getAliTradeType().equals(aliTradeType)) {
                return tradeTypeEnum;
            }
        }
        return PC;
    }

    /**
     * 根据微信交易类型获取枚举
     *
     * @param wxTradeType 微信交易类型
     * @return TradeTypeEnum
     */
    public static TradeTypeEnum getByWxTradeType(String wxTradeType) {
        for (TradeTypeEnum tradeTypeEnum : values()) {
            if (tradeTypeEnum.getWxTradeType().equals(wxTradeType)) {
                return tradeTypeEnum;
            }
        }
        return PC;
    }
}