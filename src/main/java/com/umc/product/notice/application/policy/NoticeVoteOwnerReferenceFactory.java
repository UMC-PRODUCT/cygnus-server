package com.umc.product.notice.application.policy;

import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * Notice vote가 Form ownership binding을 만들 때 사용하는 trusted factory다.
 *
 * <p>owner 좌표는 HTTP 요청에서 받지 않고 Notice ID로만 server-side에서 결정한다.
 * Form ID는 Form이 저장한 값만 factory에 전달되며 Notice ID는 호출 시점에 캡처된다.</p>
 */
public final class NoticeVoteOwnerReferenceFactory implements FormOwnerReferenceFactory {

    public static final String NAMESPACE = "notice.vote";
    public static final String SLOT = "default";
    public static final String DEFAULT_SLOT = SLOT;

    private final Long noticeId;

    private NoticeVoteOwnerReferenceFactory(Long noticeId) {
        this.noticeId = requirePositive(noticeId, "noticeId");
    }

    /**
     * {@code notice.vote/{noticeId}/default} 좌표를 만드는 Form owner factory를 반환한다.
     */
    public static NoticeVoteOwnerReferenceFactory forNotice(Long noticeId) {
        return new NoticeVoteOwnerReferenceFactory(noticeId);
    }

    @Override
    public FormOwnerReference create(Long savedFormId) {
        return FormOwnerReference.of(
            requirePositive(savedFormId, "formId"),
            NAMESPACE,
            noticeId.toString(),
            SLOT
        );
    }

    /**
     * 저장된 Form ID와 trusted Notice ID로 이미 binding된 owner reference를 만든다.
     */
    public static FormOwnerReference expectedOwner(Long noticeId, Long formId) {
        return forNotice(noticeId).create(formId);
    }

    public static String coordinate(Long noticeId) {
        requirePositive(noticeId, "noticeId");
        return NAMESPACE + "/" + noticeId + "/" + SLOT;
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
        return value;
    }
}
