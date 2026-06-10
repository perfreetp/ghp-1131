package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskSceneEnum {

    COUPON_RECEIVE(1, "领券", "同一会员短时间大量领券"),
    POS_VALIDATE(2, "试算", "同一设备频繁试算"),
    REFUND_RETURN(3, "退款返还", "异常退款返还"),
    CROSS_STORE_REDEEM(4, "跨店核销", "跨门店高频核销");

    private final Integer code;

    private final String name;

    private final String desc;

    public static RiskSceneEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
