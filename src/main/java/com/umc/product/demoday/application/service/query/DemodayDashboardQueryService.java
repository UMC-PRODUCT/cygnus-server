package com.umc.product.demoday.application.service.query;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.query.GetDemodayDashboardUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo.RankingInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo.StampHeatmapInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo.SummaryInfo;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayDashboardPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;

import lombok.RequiredArgsConstructor;

/**
 * Poll 대시보드 스냅샷을 조립한다.
 *
 * <p>요약 지표·투표 랭킹·스탬프 히트맵은 서로 다른 소스(부스 전체 목록, 득표 집계, 스탬프 집계, 프로젝트 이름)에서
 * 왔지만 하나의 스냅샷으로 묶여야 한다. 그래서 세 값을 각자 조회하는 별도 API 대신 이 서비스가 한 트랜잭션에서
 * 모두 모아 한 번에 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemodayDashboardQueryService implements GetDemodayDashboardUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayDashboardPort loadDemodayDashboardPort;
    private final GetProjectUseCase getProjectUseCase;
    private final Clock clock;

    @Override
    public DemodayDashboardInfo getDashboard(Long pollId, Long memberId) {
        DemodayPoll poll = loadDemodayPollPort.findById(pollId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        adminAccessChecker.validateAdminAccess(memberId, poll.getGisuId());

        List<DemodayBooth> booths = loadDemodayBoothPort.listByPollId(pollId);
        Map<Long, Long> voteCounts = loadDemodayDashboardPort.countActiveVotesByBooth(pollId);
        Map<Long, Long> stampCounts = loadDemodayDashboardPort.countActiveStampsByBooth(pollId);
        Map<Long, ProjectInfo> projectsById = getProjectUseCase.findAllByIds(projectIdsOf(booths));

        int totalVoteCount = voteCounts
            .values()
            .stream()
            .mapToInt(Long::intValue)
            .sum();

        return new DemodayDashboardInfo(
            pollId,
            Instant.now(clock),
            new SummaryInfo(booths.size(), totalVoteCount),
            buildRankings(booths, voteCounts, projectsById),
            buildStampHeatmap(booths, stampCounts, projectsById)
        );
    }

    private Set<Long> projectIdsOf(List<DemodayBooth> booths) {
        return booths.stream()
            .map(DemodayBooth::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * 득표 내림차순, 동점이면 boothCode 오름차순으로 정렬한 뒤 표준 경쟁 순위를 매긴다.
     *
     * <p>동점 부스는 같은 rank를 공유하고, 다음 rank는 동점자 수만큼 건너뛴다(1, 2, 2, 4).
     * 시상 발표에서 "공동 2위"를 그대로 표현할 수 있어야 하기 때문이다.
     */
    private List<RankingInfo> buildRankings(
        List<DemodayBooth> booths,
        Map<Long, Long> voteCounts,
        Map<Long, ProjectInfo> projectsById
    ) {
        List<DemodayBooth> sorted = booths.stream()
            .sorted(Comparator
                .comparingLong((DemodayBooth booth) -> voteCountOf(booth, voteCounts))
                .reversed()
                .thenComparing(DemodayBooth::getBoothCode))
            .toList();

        List<RankingInfo> rankings = new ArrayList<>();
        int rank = 0;
        int position = 0;
        int previousVoteCount = -1;
        for (DemodayBooth booth : sorted) {
            position++;
            int voteCount = voteCountOf(booth, voteCounts);
            if (voteCount != previousVoteCount) {
                rank = position;
                previousVoteCount = voteCount;
            }
            rankings.add(new RankingInfo(
                rank,
                booth.getId(),
                booth.getBoothCode(),
                booth.getProjectId(),
                resolveDisplayName(booth, projectsById),
                voteCount
            ));
        }
        return rankings;
    }

    private List<StampHeatmapInfo> buildStampHeatmap(
        List<DemodayBooth> booths,
        Map<Long, Long> stampCounts,
        Map<Long, ProjectInfo> projectsById
    ) {
        return booths.stream()
            .sorted(Comparator.comparing(DemodayBooth::getBoothCode))
            .map(booth -> new StampHeatmapInfo(
                booth.getId(),
                booth.getBoothCode(),
                booth.getProjectId(),
                resolveDisplayName(booth, projectsById),
                stampCounts.getOrDefault(booth.getId(), 0L).intValue()
            ))
            .toList();
    }

    private int voteCountOf(DemodayBooth booth, Map<Long, Long> voteCounts) {
        return voteCounts.getOrDefault(booth.getId(), 0L).intValue();
    }

    /**
     * 외부 부스는 자기 displayName을 그대로 쓰고, 프로젝트 부스는 project 도메인의 이름을 쓴다.
     *
     * <p>{@link DemodayBooth#forProject}는 displayName을 채우지 않으므로 프로젝트 부스는 이 해석 없이는
     * 이름이 비어 있다. 참조하는 프로젝트를 찾지 못한 경우(정합성이 깨진 예외 상황)에도 대시보드 전체가
     * 실패하지 않도록 booth의 displayName으로 대체한다.
     */
    private String resolveDisplayName(DemodayBooth booth, Map<Long, ProjectInfo> projectsById) {
        if (booth.getProjectId() == null) {
            return booth.getDisplayName();
        }

        ProjectInfo projectInfo = projectsById.get(booth.getProjectId());
        return projectInfo != null ? projectInfo.name() : booth.getDisplayName();
    }
}
