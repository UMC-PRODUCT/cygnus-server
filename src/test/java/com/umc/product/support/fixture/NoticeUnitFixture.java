package com.umc.product.support.fixture;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeTarget;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;

public final class NoticeUnitFixture {

    private NoticeUnitFixture() {
    }

    public static Notice notice() {
        return notice(1L, 10L);
    }

    public static Notice notice(Long id, Long authorMemberId) {
        Notice notice = Notice.create("테스트 공지", "테스트 내용", authorMemberId, false, false);
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    public static NoticeTargetInfo challengerTarget() {
        return new NoticeTargetInfo(1L, null, null, List.of(ChallengerPart.SPRINGBOOT), NoticeTab.CHALLENGER);
    }

    public static NoticeTarget target() {
        return NoticeTarget.builder()
            .noticeId(1L)
            .targetGisuId(1L)
            .targetChallengerPart(List.of(ChallengerPart.SPRINGBOOT))
            .targetNoticeTab(NoticeTab.CHALLENGER)
            .build();
    }
}
