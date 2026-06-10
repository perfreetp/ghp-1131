package com.smartretail.mbc.common.util;

public class RedisKeyUtil {

    private static final String KEY_SEPARATOR = ":";

    private static final String MBC_PREFIX = "mbc";

    private static final String MEMBER_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "member";

    private static final String POINT_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "point";

    private static final String COUPON_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "coupon";

    private static final String BENEFIT_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "benefit";

    private static final String LOCK_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "lock";

    private static final String LIMIT_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "limit";

    private static final String IDEMPOTENT_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "idempotent";

    private static final String RISK_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "risk";

    private static final String OFFLINE_PREFIX = MBC_PREFIX + KEY_SEPARATOR + "offline";

    public static String memberCode(String memberCode) {
        return MEMBER_PREFIX + KEY_SEPARATOR + "code" + KEY_SEPARATOR + memberCode;
    }

    public static String phone(String phone) {
        return MEMBER_PREFIX + KEY_SEPARATOR + "phone" + KEY_SEPARATOR + phone;
    }

    public static String pointLock(Long memberId) {
        return LOCK_PREFIX + KEY_SEPARATOR + "point" + KEY_SEPARATOR + memberId;
    }

    public static String couponLock(Long memberCouponId) {
        return LOCK_PREFIX + KEY_SEPARATOR + "coupon" + KEY_SEPARATOR + memberCouponId;
    }

    public static String benefitLock(Long memberId) {
        return LOCK_PREFIX + KEY_SEPARATOR + "benefit" + KEY_SEPARATOR + memberId;
    }

    public static String dailyLimit(String type, Long memberId) {
        return LIMIT_PREFIX + KEY_SEPARATOR + type + KEY_SEPARATOR + memberId + KEY_SEPARATOR + "daily";
    }

    public static String dailyLimit(Long templateId, Long memberId) {
        return LIMIT_PREFIX + KEY_SEPARATOR + "coupon" + KEY_SEPARATOR + templateId + KEY_SEPARATOR + memberId + KEY_SEPARATOR + "daily";
    }

    public static String couponTemplateLock(Long templateId) {
        return LOCK_PREFIX + KEY_SEPARATOR + "coupon-template" + KEY_SEPARATOR + templateId;
    }

    public static String idempotent(String requestId) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + requestId;
    }

    public static String idemBenefitLock(String orderNo) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + "benefit" + KEY_SEPARATOR + "lock" + KEY_SEPARATOR + orderNo;
    }

    public static String idemBenefitConfirm(String orderNo) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + "benefit" + KEY_SEPARATOR + "confirm" + KEY_SEPARATOR + orderNo;
    }

    public static String idemBenefitReturn(String refundNo) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + "benefit" + KEY_SEPARATOR + "return" + KEY_SEPARATOR + refundNo;
    }

    public static String idemOrderPay(String orderNo) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + "order" + KEY_SEPARATOR + "pay" + KEY_SEPARATOR + orderNo;
    }

    public static String idemOrderComplete(String orderNo) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + "order" + KEY_SEPARATOR + "complete" + KEY_SEPARATOR + orderNo;
    }

    public static String idemOrderRefund(String refundNo) {
        return IDEMPOTENT_PREFIX + KEY_SEPARATOR + "order" + KEY_SEPARATOR + "refund" + KEY_SEPARATOR + refundNo;
    }

    public static String riskCount(Integer scene, String identity) {
        return RISK_PREFIX + KEY_SEPARATOR + "count" + KEY_SEPARATOR + scene + KEY_SEPARATOR + identity;
    }

    public static String offlineSync(String offlineLockNo) {
        return OFFLINE_PREFIX + KEY_SEPARATOR + "sync" + KEY_SEPARATOR + offlineLockNo;
    }
}
