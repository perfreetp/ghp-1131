package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ReconcileStatusEnum {

    MATCHED(1, "已匹配", "对账一致"),
    UNMATCHED_AMOUNT(2, "金额不符", "权益金额与收银流水不一致"),
    UNMATCHED_MISSING(3, "流水缺失", "权益记录无对应收银流水"),
    UNMATCHED_DUPLICATE(4, "重复核销", "同一权益被多次核销"),
    UNMATCHED_REFUND_MISMATCH(5, "退款异常", "退款金额与返还权益不匹配"),
    UNMATCHED_PENDING(6, "待对账", "尚未完成对账");

    private final Integer code;

    private final String name;

    private final String desc;

    public static ReconcileStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
