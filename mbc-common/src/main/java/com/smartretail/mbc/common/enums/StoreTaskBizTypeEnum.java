package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StoreTaskBizTypeEnum {

    COUPON(1, "券", "优惠券相关业务"),
    POINT(2, "积分", "积分相关业务"),
    ORDER(3, "订单", "订单相关业务");

    private final Integer code;

    private final String name;

    private final String desc;

    public static StoreTaskBizTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
