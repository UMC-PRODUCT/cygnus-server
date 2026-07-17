package com.umc.product.notice.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("NoticeVoteFormOwnerPolicy")
class NoticeVoteFormOwnerPolicyTest {

    private static final Long NOTICE_ID = 42L;
    private static final Long FORM_ID = 900L;
    private static final Long MEMBER_ID = 7L;
    private static final FormOwnerReference OWNER = FormOwnerReference.of(
        FORM_ID,
        NoticeVoteOwnerReferenceFactory.NAMESPACE,
        NOTICE_ID.toString(),
        NoticeVoteOwnerReferenceFactory.SLOT
    );

    private CheckPermissionUseCase checkPermissionUseCase;
    private NoticeVoteFormOwnerPolicy sut;

    @BeforeEach
    void setUp() {
        checkPermissionUseCase = mock(CheckPermissionUseCase.class);
        sut = new NoticeVoteFormOwnerPolicy(checkPermissionUseCase);
    }

    @Test
    @DisplayName("canonical Notice owner와 인증 actor는 모든 Notice Form operation 권한으로 위임된다")
    void allowsCanonicalOwnerWithAuthenticatedActor() {
        given(checkPermissionUseCase.check(MEMBER_ID, permissionFor(FormOperation.READ)))
            .willReturn(true);

        assertThat(sut.allows(OWNER, FormOperation.READ, FormActorContext.authenticated(MEMBER_ID))).isTrue();
        verify(checkPermissionUseCase).check(MEMBER_ID, permissionFor(FormOperation.READ));
    }

    @Test
    @DisplayName("anonymous actor 또는 malformed owner tuple은 fail-closed한다")
    void deniesAnonymousOrMalformedOwner() {
        FormOwnerReference malformed = FormOwnerReference.of(
            FORM_ID,
            NoticeVoteOwnerReferenceFactory.NAMESPACE,
            "0042",
            NoticeVoteOwnerReferenceFactory.SLOT
        );

        assertThat(sut.allows(OWNER, FormOperation.READ, FormActorContext.anonymous())).isFalse();
        assertThat(sut.allows(malformed, FormOperation.READ, FormActorContext.authenticated(MEMBER_ID)))
            .isFalse();
        verify(checkPermissionUseCase, never()).check(MEMBER_ID, permissionFor(FormOperation.READ));
    }

    @Test
    @DisplayName("권한이 없는 actor와 다른 Notice owner는 거부된다")
    void deniesWrongActorOrNoticeOwner() {
        given(checkPermissionUseCase.check(MEMBER_ID, permissionFor(FormOperation.READ)))
            .willReturn(false);

        FormOwnerReference otherNotice = NoticeVoteOwnerReferenceFactory.expectedOwner(43L, FORM_ID);

        assertThat(sut.allows(OWNER, FormOperation.READ, FormActorContext.authenticated(MEMBER_ID))).isFalse();
        assertThat(sut.allows(otherNotice, FormOperation.READ, FormActorContext.authenticated(MEMBER_ID)))
            .isFalse();
    }

    private ResourcePermission permissionFor(FormOperation operation) {
        return ResourcePermission.of(
            com.umc.product.authorization.domain.ResourceType.NOTICE,
            NOTICE_ID,
            switch (operation) {
                case DELETE -> com.umc.product.authorization.domain.PermissionType.DELETE;
                case MANAGE_STRUCTURE, PUBLISH, READ_RESPONSES ->
                    com.umc.product.authorization.domain.PermissionType.EDIT;
                case READ, RESPOND -> com.umc.product.authorization.domain.PermissionType.READ;
            }
        );
    }
}
