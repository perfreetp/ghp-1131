package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    BIRTHDAY_BENEFIT(1, "生日权益"),
    COUPON_EXPIRE(2, "券到期提醒"),
    POINT_EXPIRE(3, "积分到期提醒"),
    LEVEL_CHANGE(4, "等级变更通知"),
    COUPON_RECEIVE(5, "领券成功通知");

    private final Integer code;

    private final String name;
}
