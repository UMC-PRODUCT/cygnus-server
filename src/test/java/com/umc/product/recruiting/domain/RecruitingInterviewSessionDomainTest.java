package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@DisplayName("Recruiting 면접 세션 도메인")
class RecruitingInterviewSessionDomainTest {

    private static final Instant ROUND_START = Instant.parse("2026-08-11T00:00:00Z");
    private static final Instant ROUND_END = Instant.parse("2026-08-15T00:00:00Z");
    private static final Instant SESSION_START = Instant.parse("2026-08-12T01:00:00Z");
    private static final Instant SESSION_END = Instant.parse("2026-08-12T03:00:00Z");

    @Test
    @DisplayName("온라인 면접 세션을 생성한다")
    void 온라인_면접_세션을_생성한다() {
        RecruitingInterviewSession session = createSession(RecruitingInterviewMode.ONLINE, "https://meet.example.com/umc");

        assertThat(session.getRoundId()).isEqualTo(10L);
        assertThat(session.getName()).isEqualTo("오전 면접");
        assertThat(session.getStartsAt()).isEqualTo(SESSION_START);
        assertThat(session.getEndsAt()).isEqualTo(SESSION_END);
        assertThat(session.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(session.getMode()).isEqualTo(RecruitingInterviewMode.ONLINE);
        assertThat(session.getLocation()).isEqualTo("https://meet.example.com/umc");
    }

    @Test
    @DisplayName("오프라인 면접 세션을 생성한다")
    void 오프라인_면접_세션을_생성한다() {
        RecruitingInterviewSession session = createSession(RecruitingInterviewMode.OFFLINE, "서울 강남구 회의실");

        assertThat(session.getMode()).isEqualTo(RecruitingInterviewMode.OFFLINE);
        assertThat(session.getLocation()).isEqualTo("서울 강남구 회의실");
    }

    @Test
    @DisplayName("세션 정보를 면접 기간 안에서 수정한다")
    void 세션_정보를_면접_기간_안에서_수정한다() {
        RecruitingInterviewSession session = createSession(RecruitingInterviewMode.ONLINE, "기존 링크");
        Instant updatedStart = Instant.parse("2026-08-13T01:00:00Z");
        Instant updatedEnd = Instant.parse("2026-08-13T04:00:00Z");

        session.update(
            "오후 면접",
            updatedStart,
            updatedEnd,
            45,
            RecruitingInterviewMode.OFFLINE,
            "서울 역삼동 회의실",
            ROUND_START,
            ROUND_END
        );

        assertThat(session.getName()).isEqualTo("오후 면접");
        assertThat(session.getStartsAt()).isEqualTo(updatedStart);
        assertThat(session.getEndsAt()).isEqualTo(updatedEnd);
        assertThat(session.getSlotDurationMinutes()).isEqualTo(45);
        assertThat(session.getMode()).isEqualTo(RecruitingInterviewMode.OFFLINE);
        assertThat(session.getLocation()).isEqualTo("서울 역삼동 회의실");
    }

    @Test
    @DisplayName("슬롯 길이는 15분 배수여야 한다")
    void 슬롯_길이는_15분_배수여야_한다() {
        assertInvalidSlot(() -> RecruitingInterviewSession.create(
            10L, "오전 면접", SESSION_START, SESSION_END, 20,
            RecruitingInterviewMode.ONLINE, "온라인", ROUND_START, ROUND_END
        ));
    }

    @Test
    @DisplayName("세션 운영 시간은 지원자 1명당 면접 시간으로 나누어떨어져야 한다")
    void 세션_운영_시간은_슬롯_길이로_나누어떨어져야_한다() {
        assertInvalidSlot(() -> RecruitingInterviewSession.create(
            10L, "오전 면접", SESSION_START, SESSION_START.plusSeconds(2700), 30,
            RecruitingInterviewMode.ONLINE, "온라인", ROUND_START, ROUND_END
        ));
    }

    @Test
    @DisplayName("세션 시작과 종료 시각은 15분 경계에 정렬되어야 한다")
    void 세션_시작과_종료_시각은_15분_경계에_정렬되어야_한다() {
        assertInvalidSlot(() -> RecruitingInterviewSession.create(
            10L,
            "오전 면접",
            Instant.parse("2026-08-12T01:05:00Z"),
            SESSION_END,
            30,
            RecruitingInterviewMode.ONLINE,
            "온라인",
            ROUND_START,
            ROUND_END
        ));
    }

    @Test
    @DisplayName("면접 장소 또는 링크는 비어 있을 수 없다")
    void 면접_장소_또는_링크는_비어_있을_수_없다() {
        assertInvalidSession(() -> createSession(RecruitingInterviewMode.OFFLINE, " "));
    }

    @Test
    @DisplayName("세션 종료 시각은 시작 시각보다 늦어야 한다")
    void 세션_종료_시각은_시작_시각보다_늦어야_한다() {
        assertInvalidSession(() -> RecruitingInterviewSession.create(
            10L, "오전 면접", SESSION_END, SESSION_START, 30,
            RecruitingInterviewMode.ONLINE, "온라인", ROUND_START, ROUND_END
        ));
    }

    @Test
    @DisplayName("세션은 모집 차수 면접 기간 밖에 생성할 수 없다")
    void 세션은_모집_차수_면접_기간_밖에_생성할_수_없다() {
        assertInvalidSession(() -> RecruitingInterviewSession.create(
            10L,
            "오전 면접",
            ROUND_START.minusSeconds(900),
            SESSION_END,
            30,
            RecruitingInterviewMode.ONLINE,
            "온라인",
            ROUND_START,
            ROUND_END
        ));
    }

    @Test
    @DisplayName("모집 차수 면접 기간 밖의 수정은 기존 세션을 변경하지 않는다")
    void 모집_차수_면접_기간_밖의_수정은_기존_세션을_변경하지_않는다() {
        RecruitingInterviewSession session = createSession(RecruitingInterviewMode.ONLINE, "기존 링크");

        assertInvalidSession(() -> session.update(
            "변경된 면접",
            SESSION_START,
            ROUND_END.plusSeconds(900),
            30,
            RecruitingInterviewMode.OFFLINE,
            "변경된 장소",
            ROUND_START,
            ROUND_END
        ));

        assertThat(session.getName()).isEqualTo("오전 면접");
        assertThat(session.getEndsAt()).isEqualTo(SESSION_END);
        assertThat(session.getMode()).isEqualTo(RecruitingInterviewMode.ONLINE);
        assertThat(session.getLocation()).isEqualTo("기존 링크");
    }

    private RecruitingInterviewSession createSession(RecruitingInterviewMode mode, String location) {
        return RecruitingInterviewSession.create(
            10L,
            "  오전 면접  ",
            SESSION_START,
            SESSION_END,
            30,
            mode,
            location,
            ROUND_START,
            ROUND_END
        );
    }

    private void assertInvalidSession(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID);
    }

    private void assertInvalidSlot(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID_SLOT);
    }
}
