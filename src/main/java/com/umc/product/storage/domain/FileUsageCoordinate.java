package com.umc.product.storage.domain;

import java.util.regex.Pattern;

/**
 * 소비 도메인 타입을 import하지 않고 파일 usage owner를 식별하는 Storage 전용 좌표입니다.
 */
public record FileUsageCoordinate(
    String usageNamespace,
    String resourceKey,
    String slot
) {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "^[a-z][a-z0-9-]{0,31}(\\.[a-z][a-z0-9-]{0,31})*$"
    );
    private static final Pattern RESOURCE_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$");
    private static final Pattern SLOT_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{0,49}$");

    public FileUsageCoordinate {
        if (usageNamespace == null
            || usageNamespace.length() > 100
            || !NAMESPACE_PATTERN.matcher(usageNamespace).matches()) {
            throw new IllegalArgumentException("파일 usage namespace 문법이 올바르지 않습니다.");
        }
        if (resourceKey == null || !RESOURCE_KEY_PATTERN.matcher(resourceKey).matches()) {
            throw new IllegalArgumentException("파일 usage resource key 문법이 올바르지 않습니다.");
        }
        if (slot == null || !SLOT_PATTERN.matcher(slot).matches()) {
            throw new IllegalArgumentException("파일 usage slot 문법이 올바르지 않습니다.");
        }
    }

    public static FileUsageCoordinate of(String usageNamespace, String resourceKey, String slot) {
        return new FileUsageCoordinate(usageNamespace, resourceKey, slot);
    }
}
