package com.umc.product.demoday.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayDashboardPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;

@ExtendWith(MockitoExtension.class)
class DemodayDashboardQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 8L;
    private static final Long POLL_ID = 10L;

    @Mock private DemodayAdminAccessChecker adminAccessChecker;
    @Mock private LoadDemodayPollPort loadDemodayPollPort;
    @Mock private LoadDemodayBoothPort loadDemodayBoothPort;
    @Mock private LoadDemodayDashboardPort loadDemodayDashboardPort;
    @Mock private GetProjectUseCase getProjectUseCase;

    private DemodayDashboardQueryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-14T06:30:00Z"), ZoneOffset.UTC);
        service = new DemodayDashboardQueryService(
            adminAccessChecker,
            loadDemodayPollPort,
            loadDemodayBoothPort,
            loadDemodayDashboardPort,
            getProjectUseCase,
            clock
        );
    }

    @Test
    @DisplayName("랭킹과 총 투표 수는 프로젝트 부스만 포함하고 동점이면 부스 코드 오름차순으로 정렬한다")
    void assignsCompetitionRanking() {
        // given
        DemodayPoll poll = mock(DemodayPoll.class);
        given(poll.getGisuId()).willReturn(GISU_ID);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        DemodayBooth booth1 = projectBooth(1L, 30, 501L);
        DemodayBooth booth2 = projectBooth(2L, 20, 502L);
        DemodayBooth booth3 = projectBooth(3L, 10, 503L);
        DemodayBooth booth4 = externalBooth(4L, 40, "외부 참가팀 A");
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of(booth1, booth2, booth3, booth4));

        given(loadDemodayDashboardPort.countActiveVotesByBooth(POLL_ID)).willReturn(Map.of(
            1L, 20L,
            2L, 15L,
            3L, 15L,
            4L, 100L // 외부 부스의 잘못된 집계 값은 랭킹과 총 투표 수에서 제외되어야 한다
        ));
        given(loadDemodayDashboardPort.countActiveStampsByBooth(POLL_ID)).willReturn(Map.of());
        given(getProjectUseCase.findAllByIds(Set.of(501L, 502L, 503L))).willReturn(Map.of(
            501L, projectInfo(501L, "잇픽"),
            502L, projectInfo(502L, "모디"),
            503L, projectInfo(503L, "토닥")
        ));

        // when
        DemodayDashboardInfo info = service.getDashboard(POLL_ID, MEMBER_ID);

        // then
        assertThat(info.summary().boothCount()).isEqualTo(4);
        assertThat(info.summary().totalVoteCount()).isEqualTo(50);

        List<DemodayDashboardInfo.RankingInfo> rankings = info.rankings();
        assertThat(rankings).containsExactly(
            new DemodayDashboardInfo.RankingInfo(1, 1L, 30, 501L, "잇픽", 20),
            new DemodayDashboardInfo.RankingInfo(2, 3L, 10, 503L, "토닥", 15),
            new DemodayDashboardInfo.RankingInfo(2, 2L, 20, 502L, "모디", 15)
        );

        verifyAdminAccessValidated();
    }

    @Test
    @DisplayName("스탬프가 0건인 부스도 부스 코드 오름차순으로 히트맵에 포함된다")
    void includesZeroStampBoothsInHeatmap() {
        // given
        DemodayPoll poll = mock(DemodayPoll.class);
        given(poll.getGisuId()).willReturn(GISU_ID);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        DemodayBooth booth1 = externalBooth(1L, 20, "부스 A");
        DemodayBooth booth2 = externalBooth(2L, 10, "부스 B");
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of(booth1, booth2));

        given(loadDemodayDashboardPort.countActiveVotesByBooth(POLL_ID)).willReturn(Map.of());
        given(loadDemodayDashboardPort.countActiveStampsByBooth(POLL_ID)).willReturn(Map.of(1L, 5L));
        given(getProjectUseCase.findAllByIds(Set.of())).willReturn(Map.of());

        // when
        DemodayDashboardInfo info = service.getDashboard(POLL_ID, MEMBER_ID);

        // then
        assertThat(info.summary().boothCount()).isEqualTo(2);
        assertThat(info.summary().totalVoteCount()).isZero();
        assertThat(info.rankings()).isEmpty();
        assertThat(info.stampHeatmap()).containsExactly(
            new DemodayDashboardInfo.StampHeatmapInfo(2L, 10, null, "부스 B", 0),
            new DemodayDashboardInfo.StampHeatmapInfo(1L, 20, null, "부스 A", 5)
        );
    }

    private void verifyAdminAccessValidated() {
        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
    }

    private DemodayBooth projectBooth(Long boothId, Integer boothCode, Long projectId) {
        DemodayBooth booth = mock(DemodayBooth.class);
        given(booth.getId()).willReturn(boothId);
        given(booth.getBoothCode()).willReturn(boothCode);
        given(booth.getProjectId()).willReturn(projectId);
        given(booth.isProjectBooth()).willReturn(true);
        return booth;
    }

    private DemodayBooth externalBooth(Long boothId, Integer boothCode, String displayName) {
        DemodayBooth booth = mock(DemodayBooth.class);
        given(booth.getId()).willReturn(boothId);
        given(booth.getBoothCode()).willReturn(boothCode);
        // Mockito는 미스텁 boxed Long을 null이 아닌 0L로 기본 응답한다. projectId가 없는
        // 부스임을 명시하기 위해 null을 직접 스텁해야 projectIdsOf()의 nonNull 필터가 정확히 동작한다.
        given(booth.getProjectId()).willReturn(null);
        given(booth.getDisplayName()).willReturn(displayName);
        given(booth.isProjectBooth()).willReturn(false);
        return booth;
    }

    private ProjectInfo projectInfo(Long projectId, String name) {
        return ProjectInfo.builder().id(projectId).name(name).build();
    }
}
