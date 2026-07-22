package com.umc.product.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.community.domain.enums.ReportTargetType;

@DisplayName("Report")
class ReportTest {

    @Test
    @DisplayName("기존 게시글 댓글 신고 모델을 그대로 보존한다")
    void create_기존_신고를_보존한다() {
        // given & when
        Report report = Report.create(1L, ReportTargetType.POST, 2L, "상세 사유");

        // then
        assertThat(report.getReporterId()).isEqualTo(1L);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.POST);
        assertThat(report.getTargetId()).isEqualTo(2L);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getReason()).isEqualTo("상세 사유");
        assertThat(report.getThreadId()).isNull();
        assertThat(report.getReasonCode()).isNull();
    }

    @Test
    @DisplayName("스레드 메시지 신고는 threadId messageId와 typed reason을 보존한다")
    void createThreadMessage_식별자와_사유를_보존한다() {
        // given & when
        Report report = Report.createThreadMessage(1L, 2L, 3L, ReportReason.ABUSE);

        // then
        assertThat(report.getReporterId()).isEqualTo(1L);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.THREAD_MESSAGE);
        assertThat(report.getThreadId()).isEqualTo(2L);
        assertThat(report.getTargetId()).isEqualTo(3L);
        assertThat(report.getReasonCode()).isEqualTo(ReportReason.ABUSE);
        assertThat(report.getReason()).isNull();
    }

    @Test
    @DisplayName("일반 factory의 THREAD_MESSAGE 사용과 thread report 필수값 누락을 거부한다")
    void rejectsInvalidThreadMessageReports() {
        assertThatThrownBy(() -> Report.create(1L, ReportTargetType.THREAD_MESSAGE, 2L, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Report.createThreadMessage(0L, 2L, 3L, ReportReason.ABUSE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Report.createThreadMessage(1L, 0L, 3L, ReportReason.ABUSE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Report.createThreadMessage(1L, 2L, 0L, ReportReason.ABUSE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Report.createThreadMessage(1L, 2L, 3L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("신고 검토 결과는 approve와 reject 상태로 명시적으로 전이한다")
    void transitionsReviewStatus() {
        Report report = Report.create(1L, ReportTargetType.POST, 2L, null);

        report.approve();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
        report.reject();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }
}
