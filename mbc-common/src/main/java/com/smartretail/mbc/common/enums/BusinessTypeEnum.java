package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum BusinessTypeEnum {

    HYPERMARKET(1, "大卖场", "大型综合超市"),
    CONVENIENCE(2, "便利店", "社区便利店"),
    FRESH(3, "生鲜专区", "生鲜水果区"),
    APPLIANCE(4, "家电专区", "家电商场"),
    CLOTHING(5, "服饰专区", "服装鞋包"),
    ONLINE(6, "线上商城", "小程序/APP");

    private final Integer code;

    private final String name;

    private final String desc;

    public static BusinessTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
