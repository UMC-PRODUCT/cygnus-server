package com.umc.product.project.application.authorization;

import java.util.Objects;

public sealed interface ProjectPolicyPrincipal
    permits ProjectPolicyPrincipal.Member, ProjectPolicyPrincipal.SystemPrincipal {

    enum Kind {
        MEMBER,
        SYSTEM
    }

    Kind kind();

    record Member(long memberId) implements ProjectPolicyPrincipal {
        @Override
        public Kind kind() {
            return Kind.MEMBER;
        }
    }

    record SystemPrincipal(String systemId) implements ProjectPolicyPrincipal {
        public SystemPrincipal {
            Objects.requireNonNull(systemId);
            if (systemId.isBlank()) {
                throw new IllegalArgumentException("systemId는 비어 있을 수 없습니다.");
            }
        }

        @Override
        public Kind kind() {
            return Kind.SYSTEM;
        }
    }
}
