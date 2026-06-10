package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PointTypeEnum {

    ADD(1, "增加"),
    SUBTRACT(2, "扣减"),
    FREEZE(3, "冻结"),
    UNFREEZE(4, "解冻");

    private final Integer code;

    private final String name;
}
