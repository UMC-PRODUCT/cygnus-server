package com.umc.product.recruiting.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

public record RecruitingApplicantEmail(String value) {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final Pattern PRACTICAL_EMAIL_PATTERN = Pattern.compile(
        "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            + "@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
            + "(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$"
    );

    public RecruitingApplicantEmail {
        String normalized = normalize(value);
        int atIndex = normalized.indexOf('@');
        if (normalized.length() > MAX_EMAIL_LENGTH
            || atIndex > MAX_LOCAL_PART_LENGTH
            || !PRACTICAL_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_EMAIL);
        }
        value = normalized;
    }

    public static RecruitingApplicantEmail from(String value) {
        return new RecruitingApplicantEmail(value);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_EMAIL);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
