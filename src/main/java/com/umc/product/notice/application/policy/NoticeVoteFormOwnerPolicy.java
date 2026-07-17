package com.umc.product.notice.application.policy;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

import lombok.RequiredArgsConstructor;

/**
 * {@code notice.vote} Form namespace의 consumer 권한 adapter다.
 *
 * <p>Form core가 Notice entity/repository를 직접 참조하지 않도록 Notice의 공개 권한
 * UseCase만 호출한다. owner 좌표가 factory의 canonical tuple이 아니거나 actor/권한 조회가
 * 실패하면 항상 거부한다.</p>
 */
@Component
@RequiredArgsConstructor
public class NoticeVoteFormOwnerPolicy implements FormOwnerPolicy {

    private final CheckPermissionUseCase checkPermissionUseCase;

    @Override
    public String namespace() {
        return NoticeVoteOwnerReferenceFactory.NAMESPACE;
    }

    @Override
    public boolean allows(
        FormOwnerReference ownerReference,
        FormOperation operation,
        FormActorContext actorContext
    ) {
        if (!isCanonical(ownerReference) || operation == null || actorContext == null) {
            return false;
        }

        Long memberId = actorContext.authenticatedMemberId().orElse(null);
        if (memberId == null) {
            return false;
        }

        try {
            return checkPermissionUseCase.check(
                memberId,
                ResourcePermission.of(ResourceType.NOTICE, noticeId(ownerReference), requiredPermission(operation))
            );
        } catch (RuntimeException ignored) {
            // 존재하지 않는 Notice, malformed authorization state 등은 consumer boundary에서
            // fail-closed한다.
            return false;
        }
    }

    private static PermissionType requiredPermission(FormOperation operation) {
        return switch (operation) {
            case DELETE -> PermissionType.DELETE;
            case MANAGE_STRUCTURE, PUBLISH, READ_RESPONSES -> PermissionType.EDIT;
            case READ, RESPOND -> PermissionType.READ;
        };
    }

    private static boolean isCanonical(FormOwnerReference ownerReference) {
        if (ownerReference == null
            || !NoticeVoteOwnerReferenceFactory.NAMESPACE.equals(ownerReference.namespace())
            || !NoticeVoteOwnerReferenceFactory.SLOT.equals(ownerReference.slot())
            || ownerReference.ownerResourceKey() == null) {
            return false;
        }

        try {
            Long noticeId = Long.valueOf(ownerReference.ownerResourceKey());
            if (noticeId <= 0 || !noticeId.toString().equals(ownerReference.ownerResourceKey())) {
                return false;
            }
            return NoticeVoteOwnerReferenceFactory.forNotice(noticeId)
                .create(ownerReference.formId())
                .sameBinding(ownerReference);
        } catch (NumberFormatException ignored) {
            return false;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static Long noticeId(FormOwnerReference ownerReference) {
        return Long.valueOf(ownerReference.ownerResourceKey());
    }
}
