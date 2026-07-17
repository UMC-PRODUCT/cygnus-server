package com.umc.product.form.domain;

import java.util.regex.Pattern;

/**
 * Form engine이 소유한 resource binding의 불변 좌표.
 *
 * <p>namespace와 tuple grammar는 DB CHECK와 동일하게 유지한다. 이 값은 외부 요청으로
 * 직접 받는 DTO가 아니라, Form application 경계에서 trusted factory로 만들어야 한다.</p>
 */
public record FormOwnerReference(
    Long formId,
    String namespace,
    String ownerResourceKey,
    String slot
) {

    public static final int NAMESPACE_MAX_LENGTH = 100;
    public static final int OWNER_RESOURCE_KEY_MAX_LENGTH = 128;
    public static final int SLOT_MAX_LENGTH = 50;

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "^[a-z][a-z0-9-]{0,31}(\\.[a-z][a-z0-9-]{0,31})*$"
    );
    private static final Pattern OWNER_RESOURCE_KEY_PATTERN = Pattern.compile(
        "^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$"
    );
    private static final Pattern SLOT_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{0,49}$");

    public FormOwnerReference {
        validatePositiveFormId(formId);
        validate(namespace, NAMESPACE_PATTERN, NAMESPACE_MAX_LENGTH, "namespace");
        validate(ownerResourceKey, OWNER_RESOURCE_KEY_PATTERN, OWNER_RESOURCE_KEY_MAX_LENGTH, "owner resource key");
        validate(slot, SLOT_PATTERN, SLOT_MAX_LENGTH, "slot");
    }

    public static FormOwnerReference of(
        Long formId,
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        return new FormOwnerReference(formId, namespace, ownerResourceKey, slot);
    }

    public static FormOwnerReference from(FormOwnership ownership) {
        if (ownership == null) {
            throw new IllegalArgumentException("form ownership은 필수입니다.");
        }
        return of(
            ownership.getFormId(),
            ownership.getNamespace(),
            ownership.getOwnerResourceKey(),
            ownership.getSlot()
        );
    }

    public FormOwnership toOwnership() {
        return FormOwnership.from(this);
    }

    public FormOwnerReference withNamespace(String replacementNamespace) {
        return of(formId, replacementNamespace, ownerResourceKey, slot);
    }

    public boolean sameBinding(FormOwnerReference other) {
        return other != null
            && formId.equals(other.formId)
            && namespace.equals(other.namespace)
            && ownerResourceKey.equals(other.ownerResourceKey)
            && slot.equals(other.slot);
    }

    private static void validatePositiveFormId(Long formId) {
        if (formId == null || formId <= 0) {
            throw new IllegalArgumentException("form id는 양수여야 합니다.");
        }
    }

    private static void validate(String value, Pattern pattern, int maxLength, String fieldName) {
        if (value == null || value.length() > maxLength || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " grammar가 올바르지 않습니다.");
        }
    }
}
