package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponTypeEnum {

    FULL_REDUCTION(1, "满减券", "满X元减Y元"),
    EXCHANGE(2, "兑换券", "凭券兑换商品");

    private final Integer code;

    private final String name;

    private final String desc;
}
