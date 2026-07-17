package com.umc.product.form.application.port.in;

import java.util.Objects;
import java.util.Optional;

/**
 * Form operation에 전달되는 server-resolved actor credential.
 *
 * <p>authenticated member와 익명 response credential은 상호 배타적이다. request DTO의 member ID나
 * owner 좌표에서 actor를 추론하는 fallback은 제공하지 않는다.</p>
 */
public final class FormActorContext {

    private static final FormActorContext ANONYMOUS = new FormActorContext(null, null);

    private final Long authenticatedMemberId;
    private final String responseAccessKey;

    private FormActorContext(Long authenticatedMemberId, String responseAccessKey) {
        this.authenticatedMemberId = authenticatedMemberId;
        this.responseAccessKey = responseAccessKey;
    }

    public static FormActorContext anonymous() {
        return ANONYMOUS;
    }

    public static FormActorContext authenticated(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("authenticated member id는 양수여야 합니다.");
        }
        return new FormActorContext(memberId, null);
    }

    public static FormActorContext responseCredential(String responseAccessKey) {
        if (responseAccessKey == null || responseAccessKey.isBlank()) {
            throw new IllegalArgumentException("response access key는 비어 있을 수 없습니다.");
        }
        return new FormActorContext(null, responseAccessKey);
    }

    public Optional<Long> authenticatedMemberId() {
        return Optional.ofNullable(authenticatedMemberId);
    }

    public Optional<String> responseAccessKey() {
        return Optional.ofNullable(responseAccessKey);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FormActorContext that)) {
            return false;
        }
        return Objects.equals(authenticatedMemberId, that.authenticatedMemberId)
            && Objects.equals(responseAccessKey, that.responseAccessKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authenticatedMemberId, responseAccessKey);
    }
}
