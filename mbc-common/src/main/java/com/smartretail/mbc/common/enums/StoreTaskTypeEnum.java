package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StoreTaskTypeEnum {

    RECONCILE_EXCEPTION(1, "核销异常", "权益核销对账异常"),
    RISK_CONFIRM(2, "风控确认", "风险订单需人工确认"),
    BUDGET_WARNING(3, "预算预警", "活动预算接近上限"),
    IDEMPOTENT_EXCEPTION(4, "幂等异常", "幂等处理超时或失败");

    private final Integer code;

    private final String name;

    private final String desc;

    public static StoreTaskTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
