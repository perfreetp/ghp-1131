package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskLevelEnum {

    SAFE(0, "安全", "无风险"),
    LOW(1, "低风险", "需关注"),
    MEDIUM(2, "中风险", "建议人工确认"),
    HIGH(3, "高风险", "建议拦截");

    private final Integer code;

    private final String name;

    private final String desc;

    public static RiskLevelEnum getByCode(Integer code) {
        if (code == null) {
            return SAFE;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(SAFE);
    }
}
