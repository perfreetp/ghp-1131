package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrowdRuleTypeEnum {

    LEVEL(1, "会员等级"),
    CONSUME_AMOUNT(2, "近30天消费金额"),
    CONSUME_COUNT(3, "近30天消费次数"),
    LAST_VISIT_DAYS(4, "最近未到店天数"),
    BIRTHDAY_MONTH(5, "生日月份"),
    COUPON_PREFERENCE(6, "券使用偏好"),
    POINT_RANGE(7, "积分区间"),
    TOTAL_SPENT(8, "累计消费金额");

    private final Integer code;

    private final String name;

    public static CrowdRuleTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
