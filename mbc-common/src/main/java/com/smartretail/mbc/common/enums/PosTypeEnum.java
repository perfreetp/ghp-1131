package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PosTypeEnum {

    STANDARD_POS(1, "标准POS", "常规收银机"),
    SELF_POS(2, "自助收银", "自助结账机"),
    MOBILE_POS(3, "移动POS", "手持扫码枪"),
    MINI_APP(4, "小程序收银", "线上下单"),
    APP_POS(5, "APP收银", "APP下单");

    private final Integer code;

    private final String name;

    private final String desc;

    public static PosTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
