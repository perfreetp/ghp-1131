package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StoreTaskPriorityEnum {

    HIGH(1, "高", "高优先级，需立即处理"),
    MEDIUM(2, "中", "中优先级，建议当日处理"),
    LOW(3, "低", "低优先级，可延后处理");

    private final Integer code;

    private final String name;

    private final String desc;

    public static StoreTaskPriorityEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
