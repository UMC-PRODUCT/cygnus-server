package com.umc.product.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.application.port.in.command.thread.report.dto.ReportCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportPageInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.SearchCommunityThreadMessageReportsQuery;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchQuery;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchResult;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.domain.enums.ReportReason;

@DisplayName("Community thread page·report DTO 경계")
class CommunityThreadPageAndReportDtoEdgeCaseTest {

    @Test
    @DisplayName("admin report query는 requester·optional filter·page 양수 계약을 검증한다")
    void validatesAdminReportQuery() {
        SearchCommunityThreadMessageReportsQuery query =
            new SearchCommunityThreadMessageReportsQuery(1L, null, null, null, null, 0, 100);
        assertThat(query.reporterId()).isNull();

        assertInvalid(() -> new SearchCommunityThreadMessageReportsQuery(
            0L, null, null, null, null, 0, 10));
        assertInvalid(() -> new SearchCommunityThreadMessageReportsQuery(
            1L, null, null, 0L, null, 0, 10));
        assertInvalid(() -> new SearchCommunityThreadMessageReportsQuery(
            1L, null, null, null, 0L, 0, 10));
        assertInvalid(() -> new SearchCommunityThreadMessageReportsQuery(
            1L, null, null, null, null, -1, 10));
        assertInvalid(() -> new SearchCommunityThreadMessageReportsQuery(
            1L, null, null, null, null, 0, 0));
        assertInvalid(() -> new SearchCommunityThreadMessageReportsQuery(
            1L, null, null, null, null, 0, 101));
    }

    @Test
    @DisplayName("persistence report query는 optional ID와 offset·limit 경계를 검증한다")
    void validatesPersistenceReportQuery() {
        ThreadMessageReportSearchQuery query =
            new ThreadMessageReportSearchQuery(null, null, null, null, 0, 1);
        assertThat(query.threadId()).isNull();
        assertThat(new ThreadMessageReportSearchQuery(null, null, 1L, 2L, 0, 100).reporterId())
            .isEqualTo(2L);

        assertInvalid(() -> new ThreadMessageReportSearchQuery(null, null, 0L, null, 0, 10));
        assertInvalid(() -> new ThreadMessageReportSearchQuery(null, null, null, 0L, 0, 10));
        assertInvalid(() -> new ThreadMessageReportSearchQuery(null, null, null, null, -1, 10));
        assertInvalid(() -> new ThreadMessageReportSearchQuery(null, null, null, null, 0, 0));
        assertInvalid(() -> new ThreadMessageReportSearchQuery(null, null, null, null, 0, 101));
    }

    @Test
    @DisplayName("report와 thread row page는 null collection을 비우고 음수 total을 거부한다")
    void validatesPageResults() {
        assertThat(new CommunityThreadMessageReportPageInfo(null, null, 0).items()).isEmpty();
        assertThat(new ThreadMessageReportSearchResult(null, 0).reports()).isEmpty();
        CommunityThreadListRows rows = new CommunityThreadListRows(null, null, 0);
        assertThat(rows.pinned()).isEmpty();
        assertThat(rows.unpinned()).isEmpty();

        assertInvalid(() -> new CommunityThreadMessageReportPageInfo(List.of(), -1, 0));
        assertInvalid(() -> new CommunityThreadMessageReportPageInfo(List.of(), null, -1));
        assertInvalid(() -> new ThreadMessageReportSearchResult(List.of(), -1));
        assertInvalid(() -> new CommunityThreadListRows(List.of(), List.of(), -1));
    }

    @Test
    @DisplayName("메시지 신고 command는 양수 ID와 필수 사유를 강제한다")
    void validatesReportCommand() {
        ReportCommunityThreadMessageCommand command =
            new ReportCommunityThreadMessageCommand(1L, 2L, ReportReason.ABUSE);
        assertThat(command.messageId()).isEqualTo(1L);

        assertInvalid(() -> new ReportCommunityThreadMessageCommand(0L, 2L, ReportReason.ABUSE));
        assertInvalid(() -> new ReportCommunityThreadMessageCommand(1L, -1L, ReportReason.ABUSE));
        assertThatThrownBy(() -> new ReportCommunityThreadMessageCommand(1L, 2L, null))
            .isInstanceOf(NullPointerException.class);
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class);
    }
}
