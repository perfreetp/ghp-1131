package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponStatusEnum {

    NOT_STARTED(0, "未开始"),
    AVAILABLE(1, "可使用"),
    USED(2, "已使用"),
    EXPIRED(3, "已过期"),
    LOCKED(4, "已锁定"),
    INACTIVE(5, "已失效");

    private final Integer code;

    private final String name;
}
