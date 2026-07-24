package com.umc.product.authorization.domain;

import java.util.Objects;

public sealed interface AuthorizationPrincipal
    permits AuthorizationPrincipal.Anonymous,
    AuthorizationPrincipal.Member,
    AuthorizationPrincipal.SystemPrincipal,
    AuthorizationPrincipal.Capability {

    Kind kind();

    enum Kind {
        ANONYMOUS,
        MEMBER,
        SYSTEM,
        CAPABILITY
    }

    record Anonymous() implements AuthorizationPrincipal {
        @Override
        public Kind kind() {
            return Kind.ANONYMOUS;
        }
    }

    record Member(long memberId) implements AuthorizationPrincipal {
        public Member {
            if (memberId <= 0) {
                throw new IllegalArgumentException("memberId는 양수여야 합니다.");
            }
        }

        @Override
        public Kind kind() {
            return Kind.MEMBER;
        }
    }

    record SystemPrincipal(String systemId) implements AuthorizationPrincipal {
        public SystemPrincipal {
            requireText(systemId, "systemId");
        }

        @Override
        public Kind kind() {
            return Kind.SYSTEM;
        }
    }

    record Capability(String capabilityType, String boundResourceId) implements AuthorizationPrincipal {
        public Capability {
            requireText(capabilityType, "capabilityType");
            requireText(boundResourceId, "boundResourceId");
        }

        @Override
        public Kind kind() {
            return Kind.CAPABILITY;
        }
    }

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
        }
    }
}
