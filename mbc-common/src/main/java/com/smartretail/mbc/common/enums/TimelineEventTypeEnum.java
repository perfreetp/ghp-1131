package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TimelineEventTypeEnum {

    REGISTER(1, "会员注册", "注册成为会员", "注册"),
    LEVEL_UP(2, "等级升级", "会员等级升级", "升级"),
    LEVEL_DOWN(3, "等级降级", "会员等级下降", "降级"),
    POINT_ADD(4, "积分增加", "积分到账", "+积分"),
    POINT_SUBTRACT(5, "积分扣减", "使用积分抵扣", "-积分"),
    COUPON_RECEIVE(6, "领取优惠券", "获得优惠券", "领券"),
    COUPON_LOCK(7, "权益锁定", "下单锁定优惠券", "锁定"),
    COUPON_USE(8, "权益核销", "使用优惠券/积分", "核销"),
    COUPON_EXPIRE(9, "券过期", "优惠券过期未使用", "过期"),
    ORDER_PAY(10, "订单支付", "订单支付成功", "支付"),
    ORDER_REFUND(11, "订单退款", "订单退款", "退款"),
    BIRTHDAY_GRANT(12, "生日权益发放", "生日积分/券到账", "生日礼"),
    MEMBER_MERGE(13, "会员合并", "合并到其他账号或被合并", "合并"),
    MESSAGE_PUSH(14, "消息推送", "收到消息推送", "消息"),
    MANUAL_REPLAY(15, "人工重放", "客服人工重放幂等请求", "重放"),
    MANUAL_MARK_FAIL(16, "人工标记失败", "客服标记处理失败", "标记失败");

    private final Integer code;

    private final String name;

    private final String desc;

    private final String tag;

    public static TimelineEventTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TimelineEventTypeEnum eventType : values()) {
            if (eventType.getCode().equals(code)) {
                return eventType;
            }
        }
        return null;
    }
}
