package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PointSourceEnum {

    CONSUME(1, "消费"),
    SIGN_IN(2, "签到"),
    BIRTHDAY(3, "生日赠送"),
    REGISTER(4, "注册赠送"),
    REFUND_RETURN(5, "退款返还"),
    ADMIN_ADJUST(6, "后台调整");

    private final Integer code;

    private final String name;
}
