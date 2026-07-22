package com.umc.product.community.application.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageAdminReportInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportPageInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.SearchCommunityThreadMessageReportsQuery;
import com.umc.product.community.application.port.out.report.SearchThreadMessageReportPort;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchQuery;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchResult;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("스레드 메시지 신고 관리자 조회 서비스")
class CommunityThreadMessageReportQueryServiceTest {

    private static final Long ADMIN_MEMBER_ID = 101L;
    private static final Long THREAD_ID = 202L;
    private static final Long REPORTER_MEMBER_ID = 303L;
    private static final Instant CREATED_AT = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;

    @Mock
    SearchThreadMessageReportPort searchThreadMessageReportPort;

    @InjectMocks
    CommunityThreadMessageReportQueryService sut;

    @Test
    @DisplayName("SUPER_ADMIN만 기본 PENDING과 모든 관리자 필터를 검색 포트에 전달하고 exact total/nextOffset을 반환한다")
    void search_SUPER_ADMIN은_기본상태와_필터_total을_전달한다() {
        // given
        SearchCommunityThreadMessageReportsQuery query = new SearchCommunityThreadMessageReportsQuery(
            ADMIN_MEMBER_ID,
            null,
            ReportReason.ABUSE,
            THREAD_ID,
            REPORTER_MEMBER_ID,
            3,
            2
        );
        ThreadMessageReportSearchQuery expectedPortQuery = new ThreadMessageReportSearchQuery(
            ReportStatus.PENDING,
            ReportReason.ABUSE,
            THREAD_ID,
            REPORTER_MEMBER_ID,
            3,
            2
        );
        Report first = report(91L, THREAD_ID, 701L, REPORTER_MEMBER_ID, ReportReason.ABUSE,
            ReportStatus.PENDING);
        Report second = report(90L, THREAD_ID, 700L, REPORTER_MEMBER_ID, ReportReason.ABUSE,
            ReportStatus.PENDING);
        given(checkChallengerAuthorityUseCase.isSuperAdmin(ADMIN_MEMBER_ID)).willReturn(true);
        given(searchThreadMessageReportPort.search(expectedPortQuery))
            .willReturn(new ThreadMessageReportSearchResult(List.of(first, second), 7L));
        // when
        CommunityThreadMessageReportPageInfo result = sut.search(query);

        // then
        assertThat(result.total()).isEqualTo(7L);
        assertThat(result.nextOffset()).isEqualTo(5);
        assertThat(result.items())
            .extracting(
                CommunityThreadMessageAdminReportInfo::reportId,
                CommunityThreadMessageAdminReportInfo::threadId,
                CommunityThreadMessageAdminReportInfo::messageId,
                CommunityThreadMessageAdminReportInfo::reason,
                CommunityThreadMessageAdminReportInfo::status,
                CommunityThreadMessageAdminReportInfo::reporterId,
                CommunityThreadMessageAdminReportInfo::createdAt
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    91L, THREAD_ID, 701L, ReportReason.ABUSE, ReportStatus.PENDING,
                    REPORTER_MEMBER_ID, CREATED_AT
                ),
                org.assertj.core.groups.Tuple.tuple(
                    90L, THREAD_ID, 700L, ReportReason.ABUSE, ReportStatus.PENDING,
                    REPORTER_MEMBER_ID, CREATED_AT
                )
            );
        then(checkChallengerAuthorityUseCase).should().isSuperAdmin(ADMIN_MEMBER_ID);
        then(searchThreadMessageReportPort).should().search(expectedPortQuery);
    }

    @Test
    @DisplayName("SUPER_ADMIN이 아니면 신고 검색 포트를 호출하지 않고 접근을 거부한다")
    void search_SUPER_ADMIN이_아니면_fail_closed한다() {
        // given
        SearchCommunityThreadMessageReportsQuery query = new SearchCommunityThreadMessageReportsQuery(
            ADMIN_MEMBER_ID,
            ReportStatus.APPROVED,
            null,
            null,
            null,
            0,
            20
        );
        given(checkChallengerAuthorityUseCase.isSuperAdmin(ADMIN_MEMBER_ID)).willReturn(false);
        // when & then
        assertThatThrownBy(() -> sut.search(query))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(searchThreadMessageReportPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마지막 페이지는 offset에 결과 수를 더해도 nextOffset을 노출하지 않는다")
    void search_마지막_페이지는_nextOffset이_null이다() {
        // given
        SearchCommunityThreadMessageReportsQuery query = new SearchCommunityThreadMessageReportsQuery(
            ADMIN_MEMBER_ID,
            ReportStatus.REJECTED,
            ReportReason.ETC,
            THREAD_ID,
            null,
            4,
            2
        );
        ThreadMessageReportSearchQuery portQuery = new ThreadMessageReportSearchQuery(
            ReportStatus.REJECTED,
            ReportReason.ETC,
            THREAD_ID,
            null,
            4,
            2
        );
        given(checkChallengerAuthorityUseCase.isSuperAdmin(ADMIN_MEMBER_ID)).willReturn(true);
        Report report = report(80L, THREAD_ID, 600L, 404L, ReportReason.ETC, ReportStatus.REJECTED);
        given(searchThreadMessageReportPort.search(portQuery))
            .willReturn(new ThreadMessageReportSearchResult(
                List.of(report),
                5L
            ));
        // when
        CommunityThreadMessageReportPageInfo result = sut.search(query);

        // then
        assertThat(result.nextOffset()).isNull();
        assertThat(result.total()).isEqualTo(5L);
    }

    @Test
    @DisplayName("서비스 경계도 음수 offset과 허용 범위 밖 limit을 방어한다")
    void search_서비스_페이지_경계를_재검증한다() {
        SearchCommunityThreadMessageReportsQuery negativeOffset =
            mock(SearchCommunityThreadMessageReportsQuery.class);
        given(negativeOffset.offset()).willReturn(-1);
        given(negativeOffset.limit()).willReturn(20);
        SearchCommunityThreadMessageReportsQuery excessiveLimit =
            mock(SearchCommunityThreadMessageReportsQuery.class);
        given(excessiveLimit.offset()).willReturn(0);
        given(excessiveLimit.limit()).willReturn(101);

        assertThatThrownBy(() -> sut.search(negativeOffset))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("offset must not be negative");
        assertThatThrownBy(() -> sut.search(excessiveLimit))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("limit must be between 1 and 100");
        then(checkChallengerAuthorityUseCase).shouldHaveNoInteractions();
        then(searchThreadMessageReportPort).shouldHaveNoInteractions();
    }

    private Report report(
        Long reportId,
        Long threadId,
        Long messageId,
        Long reporterId,
        ReportReason reason,
        ReportStatus status
    ) {
        Report report = mock(Report.class);
        given(report.getId()).willReturn(reportId);
        given(report.getThreadId()).willReturn(threadId);
        given(report.getTargetId()).willReturn(messageId);
        given(report.getReporterId()).willReturn(reporterId);
        given(report.getReasonCode()).willReturn(reason);
        given(report.getStatus()).willReturn(status);
        given(report.getCreatedAt()).willReturn(CREATED_AT);
        return report;
    }
}
