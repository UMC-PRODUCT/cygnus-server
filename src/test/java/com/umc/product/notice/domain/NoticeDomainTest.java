package com.umc.product.notice.domain;

import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static com.umc.product.support.fixture.NoticeUnitFixture.target;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.notice.domain.enums.NoticeContentType;
import com.umc.product.notice.domain.enums.NoticeReadStatus;
import com.umc.product.notice.domain.enums.NoticeReadStatusFilterType;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.enums.NoticeTargetPattern;
import com.umc.product.notice.domain.enums.VoteStatus;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;

@DisplayName("Notice 도메인 테스트")
class NoticeDomainTest {

    @Test
    @DisplayName("공지는 수정, 작성자 검증, 알림 상태 전이를 지원한다")
    void 공지는_수정과_알림_상태를_관리한다() {
        Notice target = Notice.create("제목", "내용", 10L, true, false);

        assertThat(target.isNotificationRequired()).isTrue();
        target.updateTitleOrContent("수정 제목", "수정 내용");
        target.updateMustRead(true);
        target.validateAuthorMember(10L);
        target.markAsNotified(Instant.parse("2026-07-22T00:00:00Z"));

        assertThat(target.getTitle()).isEqualTo("수정 제목");
        assertThat(target.getContent()).isEqualTo("수정 내용");
        assertThat(target.isMustRead()).isTrue();
        assertThat(target.isNotificationRequired()).isFalse();
        assertThatThrownBy(() -> target.validateAuthorMember(11L)).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("공지 부가 콘텐츠는 공지와 표시 순서를 보존한다")
    void 부가_콘텐츠는_공지와_순서를_보존한다() {
        Notice target = notice();
        NoticeImage image = NoticeImage.create("image-id", target, 1);
        NoticeLink link = NoticeLink.create("https://example.com", target, 2);
        NoticeRead read = NoticeRead.builder().notice(target).challengerId(3L).build();

        assertThat(image.getNotice()).isSameAs(target);
        assertThat(image.getImageId()).isEqualTo("image-id");
        assertThat(image.getDisplayOrder()).isEqualTo(1);
        assertThat(link.getNotice()).isSameAs(target);
        assertThat(link.getLink()).isEqualTo("https://example.com");
        assertThat(link.getDisplayOrder()).isEqualTo(2);
        assertThat(read.getNotice()).isSameAs(target);
        assertThat(read.getChallengerId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("투표는 시작 전, 진행 중, 종료 경계를 exclusive 종료 시각으로 판정한다")
    void 투표는_시간_경계를_판정한다() {
        Instant startsAt = Instant.parse("2026-07-22T00:00:00Z");
        Instant endsAt = Instant.parse("2026-07-23T00:00:00Z");
        NoticeVote vote = NoticeVote.create(2L, notice(), startsAt, endsAt);

        assertThat(vote.getVoteId()).isEqualTo(2L);
        assertThat(vote.getOpenStatus(startsAt.minusNanos(1))).isEqualTo(VoteStatus.NOT_STARTED);
        assertThat(vote.getOpenStatus(startsAt)).isEqualTo(VoteStatus.OPEN);
        assertThat(vote.getOpenStatus(endsAt.minusNanos(1))).isEqualTo(VoteStatus.OPEN);
        assertThat(vote.getOpenStatus(endsAt)).isEqualTo(VoteStatus.CLOSED);
    }

    @Test
    @DisplayName("공지 대상은 엔티티 변환, staff 판정, 전체 및 세부 scope 일치를 지원한다")
    void 공지_대상은_scope를_판정한다() {
        NoticeTarget entity = target();
        NoticeTargetInfo info = NoticeTargetInfo.from(entity);

        assertThat(info.isStaffNotice()).isFalse();
        assertThat(info.isTarget(1L, 2L, 3L, ChallengerPart.SPRINGBOOT)).isTrue();
        assertThat(info.isTarget(2L, 2L, 3L, ChallengerPart.SPRINGBOOT)).isFalse();

        entity.update(2L, 3L, 4L, List.of(ChallengerPart.WEB), NoticeTab.SCHOOL_PART_LEADER);
        NoticeTargetInfo updated = NoticeTargetInfo.from(entity);
        assertThat(updated.isStaffNotice()).isTrue();
        assertThat(updated.isTarget(2L, 3L, 4L, ChallengerPart.WEB)).isTrue();
        assertThat(updated.isTarget(2L, 9L, 4L, ChallengerPart.WEB)).isFalse();
        assertThat(new NoticeTargetInfo(null, null, null, null, null).isStaffNotice()).isFalse();
    }

    @Test
    @DisplayName("공지 분류는 challenger 여부와 target info 변환을 제공한다")
    void 공지_분류를_target으로_변환한다() {
        NoticeClassification classification = new NoticeClassification(
            1L, 2L, 3L, ChallengerPart.SPRINGBOOT, NoticeTab.CHALLENGER
        );
        assertThat(classification.isChallengerQuery()).isTrue();
        assertThat(classification.toTargetInfo().targetParts()).containsExactly(ChallengerPart.SPRINGBOOT);
        assertThat(new NoticeClassification(1L, null, null, null, NoticeTab.SCHOOL_CORE).isChallengerQuery())
            .isFalse();
        assertThat(new NoticeClassification(1L, null, null, null, NoticeTab.CHALLENGER)
            .toTargetInfo().targetParts()).isNull();
    }

    @Test
    @DisplayName("공지 tab은 읽기 계층과 challenger role 변환을 정확히 제공한다")
    void 공지_tab은_역할_계층을_판정한다() {
        assertThat(NoticeTab.staffRolesReadableBy(null)).isEmpty();
        assertThat(NoticeTab.staffRolesReadableBy(NoticeTab.CHALLENGER)).isEmpty();
        assertThat(NoticeTab.staffRolesReadableBy(NoticeTab.SCHOOL_CORE))
            .containsExactly(NoticeTab.SCHOOL_CORE, NoticeTab.SCHOOL_PART_LEADER);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.SCHOOL_PART_LEADER))
            .contains(NoticeTab.SCHOOL_PART_LEADER);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.SCHOOL_ETC_ADMIN))
            .contains(NoticeTab.SCHOOL_PART_LEADER);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.SCHOOL_PRESIDENT)).contains(NoticeTab.SCHOOL_CORE);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.SCHOOL_VICE_PRESIDENT)).contains(NoticeTab.SCHOOL_CORE);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.CHAPTER_PRESIDENT)).contains(NoticeTab.SCHOOL_CORE);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER))
            .contains(NoticeTab.CENTRAL_MEMBER);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER))
            .contains(NoticeTab.CENTRAL_MEMBER);
        assertThat(NoticeTab.findFrom(ChallengerRoleType.CENTRAL_PRESIDENT)).isEmpty();
        assertThat(NoticeTab.findFrom(ChallengerRoleType.CENTRAL_VICE_PRESIDENT)).isEmpty();
        assertThat(NoticeTab.CENTRAL_MEMBER.isStaffRole()).isTrue();
        assertThat(NoticeTab.CHALLENGER.isStaffRole()).isFalse();
        assertThat(NoticeTab.SCHOOL_PART_LEADER.includes(NoticeTab.SCHOOL_CORE)).isTrue();
        assertThat(NoticeTab.SCHOOL_CORE.includes(null)).isFalse();
        assertThat(NoticeTab.SCHOOL_CORE.includes(NoticeTab.CHALLENGER)).isFalse();
    }

    @Test
    @DisplayName("모든 일반 challenger 대상 패턴은 구조와 권한 호출을 검증한다")
    void 모든_challenger_대상_패턴을_검증한다() {
        GetChallengerRoleUseCase useCase = org.mockito.Mockito.mock(GetChallengerRoleUseCase.class);
        assertPattern(info(null, null, null, null, NoticeTab.CHALLENGER),
            NoticeTargetPattern.ALL_GISU_ALL_TARGET, useCase);
        assertPattern(info(null, null, 3L, null, NoticeTab.CHALLENGER),
            NoticeTargetPattern.ALL_GISU_SPECIFIC_SCHOOL, useCase);
        assertPattern(info(1L, null, null, null, NoticeTab.CHALLENGER),
            NoticeTargetPattern.SPECIFIC_GISU_ALL_TARGET, useCase);
        assertPattern(info(1L, null, null, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER),
            NoticeTargetPattern.SPECIFIC_GISU_SPECIFIC_PART, useCase);
        assertPattern(info(1L, null, 3L, null, NoticeTab.CHALLENGER),
            NoticeTargetPattern.SPECIFIC_GISU_SPECIFIC_SCHOOL, useCase);
        assertPattern(info(1L, null, 3L, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER),
            NoticeTargetPattern.SPECIFIC_GISU_SPECIFIC_SCHOOL_WITH_PART, useCase);
        assertPattern(info(1L, 2L, null, null, NoticeTab.CHALLENGER),
            NoticeTargetPattern.SPECIFIC_GISU_SPECIFIC_CHAPTER, useCase);
        assertPattern(info(1L, 2L, null, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER),
            NoticeTargetPattern.SPECIFIC_GISU_SPECIFIC_CHAPTER_WITH_PART, useCase);
    }

    @Test
    @DisplayName("잘못된 challenger 대상 조합은 구체적인 domain 예외로 거부한다")
    void 잘못된_challenger_대상_조합을_거부한다() {
        GetChallengerRoleUseCase useCase = org.mockito.Mockito.mock(GetChallengerRoleUseCase.class);
        assertInvalid(info(null, null, 3L, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER), useCase);
        assertInvalid(info(null, 2L, null, null, NoticeTab.CHALLENGER), useCase);
        assertInvalid(info(1L, 2L, 3L, null, NoticeTab.CHALLENGER), useCase);
        assertThatThrownBy(() -> NoticeTargetPattern.from(
            info(null, null, null, List.of(ChallengerPart.WEB), NoticeTab.CHALLENGER)
        )).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("staff 대상 패턴은 기수·학교·파트 제약과 작성 권한 분기를 검증한다")
    void staff_대상_패턴을_검증한다() {
        GetChallengerRoleUseCase useCase = org.mockito.Mockito.mock(GetChallengerRoleUseCase.class);
        assertPattern(info(1L, null, null, null, NoticeTab.CENTRAL_MEMBER),
            NoticeTargetPattern.STAFF_SPECIFIC_GISU, useCase);
        assertPattern(info(1L, null, null, List.of(ChallengerPart.WEB), NoticeTab.SCHOOL_PART_LEADER),
            NoticeTargetPattern.STAFF_SPECIFIC_GISU_SPECIFIC_PART, useCase);
        assertPattern(info(1L, null, 3L, null, NoticeTab.SCHOOL_PART_LEADER),
            NoticeTargetPattern.STAFF_SPECIFIC_GISU_SPECIFIC_SCHOOL, useCase);
        assertPattern(info(1L, null, null, null, NoticeTab.SCHOOL_CORE),
            NoticeTargetPattern.STAFF_SPECIFIC_GISU, useCase);

        assertThatThrownBy(() -> NoticeTargetPattern.from(info(null, null, null, null, NoticeTab.SCHOOL_CORE)))
            .isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> NoticeTargetPattern.from(info(1L, 2L, null, null, NoticeTab.SCHOOL_CORE)))
            .isInstanceOf(NoticeDomainException.class);
        assertInvalid(info(1L, null, 3L, null, NoticeTab.CENTRAL_MEMBER), useCase);
        assertInvalid(info(1L, null, null, List.of(ChallengerPart.WEB), NoticeTab.SCHOOL_CORE), useCase);
    }

    @Test
    @DisplayName("공지 enum과 error code는 전체 상수를 안정적으로 노출한다")
    void enum과_error_code를_노출한다() {
        assertThat(NoticeContentType.values()).containsExactly(
            NoticeContentType.LINK, NoticeContentType.IMAGE, NoticeContentType.VOTE
        );
        assertThat(NoticeReadStatus.values()).containsExactly(NoticeReadStatus.READ, NoticeReadStatus.UNREAD);
        assertThat(NoticeReadStatusFilterType.values()).containsExactly(
            NoticeReadStatusFilterType.CHAPTER,
            NoticeReadStatusFilterType.SCHOOL,
            NoticeReadStatusFilterType.ALL
        );
        assertThat(VoteStatus.values()).containsExactly(VoteStatus.NOT_STARTED, VoteStatus.OPEN, VoteStatus.CLOSED);
        assertThat(NoticeErrorCode.values()).allSatisfy(code -> {
            assertThat(code.getHttpStatus()).isNotNull();
            assertThat(code.getCode()).startsWith("NOTICE-");
            assertThat(code.getMessage()).isNotBlank();
        });
        NoticeDomainException custom = new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND, "custom");
        assertThat(custom.getMessage()).isEqualTo("custom");
    }

    private static void assertPattern(
        NoticeTargetInfo info,
        NoticeTargetPattern expected,
        GetChallengerRoleUseCase useCase
    ) {
        NoticeTargetPattern pattern = NoticeTargetPattern.from(info);
        assertThat(pattern).isEqualTo(expected);
        pattern.validatePermission(info, 10L, useCase);
    }

    private static void assertInvalid(NoticeTargetInfo info, GetChallengerRoleUseCase useCase) {
        assertThatThrownBy(() -> NoticeTargetPattern.from(info).validatePermission(info, 10L, useCase))
            .isInstanceOf(NoticeDomainException.class);
    }

    private static NoticeTargetInfo info(
        Long gisuId,
        Long chapterId,
        Long schoolId,
        List<ChallengerPart> parts,
        NoticeTab tab
    ) {
        return new NoticeTargetInfo(gisuId, chapterId, schoolId, parts, tab);
    }
}
