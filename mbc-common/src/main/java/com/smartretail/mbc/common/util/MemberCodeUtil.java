package com.smartretail.mbc.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class MemberCodeUtil {

    private static final String PREFIX = "M";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Pattern MEMBER_CODE_PATTERN = Pattern.compile("^M\\d{14}$");

    public static String generateMemberCode() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        return PREFIX + datePart + randomPart;
    }

    public static boolean validateMemberCode(String code) {
        if (code == null || code.length() != 15) {
            return false;
        }
        return MEMBER_CODE_PATTERN.matcher(code).matches();
    }
}
