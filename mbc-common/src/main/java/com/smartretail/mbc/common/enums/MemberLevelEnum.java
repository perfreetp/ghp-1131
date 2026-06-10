package com.smartretail.mbc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MemberLevelEnum {

    BRONZE(1, "青铜", 0, "累计消费0元"),
    SILVER(2, "白银", 500, "累计消费500元"),
    GOLD(3, "黄金", 2000, "累计消费2000元"),
    PLATINUM(4, "铂金", 5000, "累计消费5000元"),
    DIAMOND(5, "钻石", 10000, "累计消费10000元");

    private final Integer code;

    private final String name;

    private final Integer threshold;

    private final String desc;

    public static MemberLevelEnum getByCode(Integer code) {
        if (code == null) {
            return BRONZE;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(BRONZE);
    }

    public static MemberLevelEnum getLevelByGrowth(Integer growth) {
        if (growth == null) {
            return BRONZE;
        }
        MemberLevelEnum result = BRONZE;
        for (MemberLevelEnum level : values()) {
            if (growth >= level.getThreshold()) {
                result = level;
            } else {
                break;
            }
        }
        return result;
    }
}
