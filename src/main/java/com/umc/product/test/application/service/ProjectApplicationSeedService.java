package com.umc.product.test.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.DecideApplicationUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectApplicationDraftUseCase;
import com.umc.product.project.application.port.in.command.dto.ApplicationDecisionStatus;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectApplicationDraftCommand;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMatchingRoundInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.test.application.port.in.command.SeedProjectApplicationsUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult.ApplicationEntry;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult.Counts;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult.FailedApplication;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult.ProjectApplications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지원서 시나리오 시딩 서비스.
 * <p>
 * 지정 매칭 차수 + 지부를 기준으로, 아직 팀에 합류하지 않은 ACTIVE 챌린저들이 IN_PROGRESS 프로젝트에
 * Draft → 더미 답변 → Submit 까지 진행한다. 그 뒤 각 챌린저의 최종 상태는
 * {@code SUBMITTED} / {@code APPROVED} / {@code REJECTED} 중 하나로 무작위 결정된다 (≈ 1/3 분포).
 * 그 결과로 운영 화면의 "검토 대기 + 합격자 + 불합격자" 분포가 자연스럽게 채워진다.
 * <p>
 * <b>전제 조건</b>: 매칭 차수가 현재 OPEN 상태(startsAt &lt;= now &lt;= endsAt)여야 한다.
 * <p>
 * <b>도메인 가드에 위임</b>: 자기지원 / 이미 팀원 / 같은 차수 중복 지원 / 파트 불일치 등은 시딩이 사전
 * 필터링하지 않고 도메인 UseCase 가 던지는 가드 예외를 그대로 받아 {@code failedApplications} 에 누적한다.
 * <p>
 * <b>ProjectMember 등록은 시딩 책임 밖</b>: APPROVED status 만 변경하고 ProjectMember 는 생성하지 않는다.
 * 매칭 완료 (APPROVED → ProjectMember 일괄 등록) 는 차수 종료 시점의 {@code autoDecide} 가 처리한다.
 * <p>
 * <b>트랜잭션 정책</b>: {@link Propagation#NOT_SUPPORTED}. 각 UseCase 가 자체 트랜잭션을 가지므로 한
 * 챌린저 시나리오가 실패해도 이전 성공 건은 commit 된다.
 */
@Slf4j
@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ProjectApplicationSeedService implements SeedProjectApplicationsUseCase {

    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetProjectMatchingRoundUseCase getProjectMatchingRoundUseCase;
    private final GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;
    private final SearchProjectUseCase searchProjectUseCase;

    private final CreateDraftProjectApplicationUseCase createDraftProjectApplicationUseCase;
    private final UpdateProjectApplicationDraftUseCase updateProjectApplicationDraftUseCase;
    private final SubmitProjectApplicationUseCase submitProjectApplicationUseCase;
    private final DecideApplicationUseCase decideApplicationUseCase;

    @Override
    public SeedProjectApplicationsResult seed(SeedProjectApplicationsCommand command) {
        long startedAt = System.currentTimeMillis();

        // 1. 매칭차수 조회 — chapterId 소속 + OPEN 검증
        ProjectMatchingRoundInfo round = getProjectMatchingRoundUseCase
            .list(command.chapterId(), null).stream()
            .filter(r -> r.id().equals(command.matchingRoundId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "matchingRoundId=%d 가 chapterId=%d 에 속하지 않습니다."
                    .formatted(command.matchingRoundId(), command.chapterId())
            ));

        Instant now = Instant.now();
        if (now.isBefore(round.startsAt()) || now.isAfter(round.endsAt())) {
            throw new IllegalArgumentException(
                "matchingRoundId=%d 는 현재 OPEN 상태가 아닙니다 (now=%s, startsAt=%s, endsAt=%s)."
                    .formatted(command.matchingRoundId(), now, round.startsAt(), round.endsAt())
            );
        }

        // 2. 차수 type 에 맞는 ACTIVE 챌린저 풀 — 사전 필터링은 part 와 status 만, 나머지는 도메인 가드 위임
        Long gisuId = getGisuUseCase.getActiveGisuId();
        Set<ChallengerPart> eligibleParts = eligibleParts(round.type());
        List<ChallengerInfo> challengers = getChallengerUseCase.getAllByGisuId(gisuId).stream()
            .filter(c -> c.challengerStatus() == ChallengerStatus.ACTIVE)
            .filter(c -> eligibleParts.contains(c.part()))
            .toList();

        if (challengers.isEmpty()) {
            log.warn("project application seed skipped: gisuId={} 에 type={} 챌린저가 없습니다.",
                gisuId, round.type());
            return emptyResult(command.matchingRoundId());
        }

        // 3. 지부의 IN_PROGRESS 프로젝트 목록 — 챌린저 시점 검색 호출자로 풀의 첫 ID 사용
        Long searcherMemberId = challengers.get(0).memberId();
        Page<ProjectInfo> projectPage = searchProjectUseCase.search(
            SearchProjectQuery.forChallenger(
                gisuId, null, command.chapterId(), null, null, null, Pageable.unpaged()
            ),
            searcherMemberId
        );
        List<ProjectInfo> projects = projectPage.getContent();

        if (projects.isEmpty()) {
            log.warn("project application seed skipped: chapterId={} 에 IN_PROGRESS 프로젝트가 없습니다.",
                command.chapterId());
            return emptyResult(command.matchingRoundId());
        }

        log.info(
            "project application seed start: chapterId={}, matchingRoundId={}, roundType={}, "
                + "projectCount={}, challengerPoolSize={}",
            command.chapterId(), command.matchingRoundId(), round.type(),
            projects.size(), challengers.size()
        );

        // 4. 각 챌린저마다 시나리오 실행
        Map<Long, List<ApplicationEntry>> applicationsByProject = new LinkedHashMap<>();
        List<FailedApplication> failures = new ArrayList<>();
        int submittedCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        for (ChallengerInfo challenger : challengers) {
            // 본인 part 모집 중이고 본인이 PO 가 아닌 프로젝트 후보 — 나머지 (이미 팀원/지원 등) 는 도메인 가드 위임
            List<ProjectInfo> candidates = projects.stream()
                .filter(p -> p.partQuotas().stream().anyMatch(q -> q.part() == challenger.part()))
                .filter(p -> !p.productOwnerMemberId().equals(challenger.memberId()))
                .toList();

            if (candidates.isEmpty()) {
                failures.add(new FailedApplication(
                    challenger.memberId(), null, null, "NO_PROJECT",
                    "파트=%s 를 모집하는 프로젝트가 없습니다.".formatted(challenger.part())
                ));
                continue;
            }

            ProjectInfo picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            Long projectId = picked.id();
            Long poMemberId = picked.productOwnerMemberId();

            ApplicationOutcome outcome = runApplicationScenario(
                challenger, projectId, poMemberId, command.matchingRoundId()
            );

            switch (outcome.step()) {
                case "SUBMITTED" -> {
                    submittedCount++;
                    recordApplication(applicationsByProject, projectId, outcome.applicationId(),
                        challenger, ProjectApplicationStatus.SUBMITTED);
                }
                case "APPROVED" -> {
                    approvedCount++;
                    recordApplication(applicationsByProject, projectId, outcome.applicationId(),
                        challenger, ProjectApplicationStatus.APPROVED);
                }
                case "REJECTED" -> {
                    rejectedCount++;
                    recordApplication(applicationsByProject, projectId, outcome.applicationId(),
                        challenger, ProjectApplicationStatus.REJECTED);
                }
                default -> failures.add(new FailedApplication(
                    challenger.memberId(), projectId, outcome.applicationId(),
                    outcome.step(), outcome.reason()
                ));
            }
        }

        long elapsed = System.currentTimeMillis() - startedAt;
        log.info(
            "project application seed completed in {}ms: submitted={}, approved={}, rejected={}, failed={}",
            elapsed, submittedCount, approvedCount, rejectedCount, failures.size()
        );

        List<ProjectApplications> createdApplications = applicationsByProject.entrySet().stream()
            .map(e -> new ProjectApplications(e.getKey(), e.getValue()))
            .toList();
        Counts counts = new Counts(submittedCount, approvedCount, rejectedCount, failures.size());
        return new SeedProjectApplicationsResult(
            command.matchingRoundId(), createdApplications, failures, counts
        );
    }

    private SeedProjectApplicationsResult emptyResult(Long matchingRoundId) {
        return new SeedProjectApplicationsResult(
            matchingRoundId, List.of(), List.of(), new Counts(0, 0, 0, 0)
        );
    }

    private void recordApplication(
        Map<Long, List<ApplicationEntry>> byProject,
        Long projectId, Long applicationId,
        ChallengerInfo challenger, ProjectApplicationStatus status
    ) {
        byProject.computeIfAbsent(projectId, k -> new ArrayList<>())
            .add(new ApplicationEntry(applicationId, challenger.memberId(), challenger.part(), status));
    }

    private ApplicationOutcome runApplicationScenario(
        ChallengerInfo challenger, Long projectId, Long poMemberId, Long matchingRoundId
    ) {
        // DRAFT 생성
        Long applicationId;
        try {
            applicationId = createDraftProjectApplicationUseCase.create(
                CreateDraftProjectApplicationCommand.builder()
                    .projectId(projectId)
                    .applicantMemberId(challenger.memberId())
                    .matchingRoundId(matchingRoundId)
                    .build()
            ).applicationId();
        } catch (Exception e) {
            log.error("seed DRAFT 실패 (memberId={}, projectId={}): {}",
                challenger.memberId(), projectId, e.toString());
            return ApplicationOutcome.fail("DRAFT", e.toString(), null);
        }

        // 더미 답변 채우기 (isRequired=true 질문 대응)
        try {
            List<UpdateProjectApplicationDraftCommand.AnswerEntry> answers =
                buildDummyAnswers(projectId, challenger.memberId());
            if (!answers.isEmpty()) {
                updateProjectApplicationDraftUseCase.update(
                    UpdateProjectApplicationDraftCommand.builder()
                        .projectId(projectId)
                        .applicationId(applicationId)
                        .requesterMemberId(challenger.memberId())
                        .answers(answers)
                        .build()
                );
            }
        } catch (Exception e) {
            log.error("seed FILL 실패 (memberId={}, projectId={}): {}",
                challenger.memberId(), projectId, e.toString());
            return ApplicationOutcome.fail("FILL", e.toString(), applicationId);
        }

        // 제출
        try {
            submitProjectApplicationUseCase.submit(
                SubmitProjectApplicationCommand.builder()
                    .projectId(projectId)
                    .applicationId(applicationId)
                    .requesterMemberId(challenger.memberId())
                    .build()
            );
        } catch (Exception e) {
            log.error("seed SUBMIT 실패 (memberId={}, projectId={}): {}",
                challenger.memberId(), projectId, e.toString());
            return ApplicationOutcome.fail("SUBMIT", e.toString(), applicationId);
        }

        // 최종 상태 무작위 결정: SUBMITTED / APPROVED / REJECTED ≈ 1/3 분포.
        // REJECTED 결정은 매칭 규칙의 최소선발 검증을 통과하는 경우에만 반영된다.
        int rand = nextDecisionIndex();
        if (rand == 0) {
            return ApplicationOutcome.success("SUBMITTED", applicationId);
        }

        ApplicationDecisionStatus decision = (rand == 1)
            ? ApplicationDecisionStatus.APPROVED
            : ApplicationDecisionStatus.REJECTED;
        try {
            decideApplicationUseCase.decide(applicationId, decision, null, poMemberId);
        } catch (Exception e) {
            log.error("seed DECIDE 실패 (memberId={}, projectId={}, decision={}): {}",
                challenger.memberId(), projectId, decision, e.toString());
            return ApplicationOutcome.fail("DECIDE", e.toString(), applicationId);
        }
        return ApplicationOutcome.success(decision.name(), applicationId);
    }

    int nextDecisionIndex() {
        return ThreadLocalRandom.current().nextInt(3);
    }

    /**
     * isRequired=true 인 질문만 추려 더미 텍스트 답변을 구성한다. 챌린저 시점으로 form 조회 — COMMON 섹션 +
     * 본인 파트 PART 섹션만 노출되어 마스킹된 섹션은 자동 제외된다.
     */
    private List<UpdateProjectApplicationDraftCommand.AnswerEntry> buildDummyAnswers(
        Long projectId, Long challengerMemberId
    ) {
        return getProjectApplicationFormUseCase
            .findByProjectId(projectId, challengerMemberId)
            .map(form -> form.sections().stream()
                .flatMap(s -> s.questions().stream())
                .filter(ApplicationFormInfo.QuestionInfo::isRequired)
                .map(q -> UpdateProjectApplicationDraftCommand.AnswerEntry.builder()
                    .questionId(q.questionId())
                    .textValue("[시딩 더미 답변]")
                    .selectedOptionIds(List.of())
                    .fileIds(List.of())
                    .build())
                .toList())
            .orElse(List.of());
    }

    private Set<ChallengerPart> eligibleParts(MatchingType type) {
        if (type == MatchingType.PLAN_DESIGN) {
            return EnumSet.of(ChallengerPart.DESIGN);
        }
        return EnumSet.of(
            ChallengerPart.WEB,
            ChallengerPart.ANDROID,
            ChallengerPart.IOS,
            ChallengerPart.NODEJS,
            ChallengerPart.SPRINGBOOT
        );
    }

    private record ApplicationOutcome(String step, String reason, Long applicationId) {

        static ApplicationOutcome success(String step, Long applicationId) {
            return new ApplicationOutcome(step, null, applicationId);
        }

        static ApplicationOutcome fail(String step, String reason, Long applicationId) {
            return new ApplicationOutcome(step, reason, applicationId);
        }
    }
}
