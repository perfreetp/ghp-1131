package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StoreTaskSourceEnum {

    SYSTEM("system", "系统生成", "系统自动生成的任务"),
    HEADQUARTERS("headquarters", "总部派发", "总部派发的任务"),
    STORE("store", "门店创建", "门店自行创建的任务");

    private final String code;

    private final String name;

    private final String desc;

    public static StoreTaskSourceEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
