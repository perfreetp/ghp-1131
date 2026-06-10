package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StoreTaskStatusEnum {

    PENDING(0, "待处理", "任务尚未处理"),
    PROCESSING(1, "处理中", "任务正在处理中"),
    DONE(2, "已处理", "任务已处理完成"),
    IGNORED(3, "已忽略", "任务已被忽略");

    private final Integer code;

    private final String name;

    private final String desc;

    public static StoreTaskStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
