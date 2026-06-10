package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    CREATE(0, "待支付"),
    PAID(1, "已支付"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款");

    private final Integer code;

    private final String name;
}
