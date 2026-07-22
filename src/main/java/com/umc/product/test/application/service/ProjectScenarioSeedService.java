package com.umc.product.test.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.UpdatePartQuotasUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.command.UpsertProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.command.dto.AddProjectMemberCommand;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectCommand;
import com.umc.product.project.application.port.in.command.dto.PublishProjectCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectCommand;
import com.umc.product.project.application.port.in.command.dto.UpdatePartQuotasCommand;
import com.umc.product.project.application.port.in.command.dto.UpdatePartQuotasCommand.Entry;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectCommand;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.test.application.port.in.command.SeedProjectScenariosUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult.CreatedProject;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult.FailedProject;
import com.umc.product.test.application.port.in.command.dto.SeedProjectScenariosResult.PartFill;
import com.umc.product.test.application.port.in.command.dto.TargetProjectStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시나리오 기반 프로젝트 시딩 서비스.
 * <p>
 * 활성 기수에 대해 {@link TargetProjectStatus} 까지 도달한 프로젝트를 N 개 생성한다. SQL 직접 주입이
 * 아니라 도메인 UseCase 시퀀스를 호출해 만들기 때문에 도메인 가드를 모두 통과한 데이터가 된다.
 * <ul>
 *   <li>{@code DRAFT}        — createDraft + updateProject(name/desc)</li>
 *   <li>{@code PENDING_REVIEW} — 위 + upsertForm + submit</li>
 *   <li>{@code IN_PROGRESS}    — 위 + updatePartQuotas + publish + addProjectMember 충원</li>
 * </ul>
 * <p>
 * PO 선정 정책: {@code productOwnerMemberIds} 가 명시되면 그 리스트로만 사용한다(size 검증,
 * 활성 기수 PLAN 챌린저 검증). null 이면 활성 기수의 ACTIVE PLAN 챌린저 풀에서 무작위로 N 명을
 * 뽑는다. 풀에 없는 멤버를 PLAN 으로 강제 등록하지는 않는다.
 * <p>
 * IN_PROGRESS 멤버 충원: PartQuota 의 각 entry 에 대해 {@code target = random(0, quota)} 만큼
 * PO 학교의 해당 파트 ACTIVE 챌린저 풀에서 무작위 추출. 풀이 부족하면 부족한 만큼만 추가하고
 * {@link PartFill#filled()} 에 실제 수치를 노출한다.
 * <p>
 * <b>트랜잭션 정책</b>: 외부 트랜잭션 차단({@link Propagation#NOT_SUPPORTED}). 각 단계 UseCase 가
 * 자체 트랜잭션을 가지므로 중간 단계가 실패해도 그 앞까지의 변경은 commit 된다. 호출자는
 * {@link FailedProject} 의 {@code reachedStatus} 로 orphan 정리 여부를 판단해야 한다.
 */
@Slf4j
@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProjectScenarioSeedService implements SeedProjectScenariosUseCase {

    private final GetGisuUseCase getGisuUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final CreateDraftProjectUseCase createDraftProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final UpsertProjectApplicationFormUseCase upsertProjectApplicationFormUseCase;
    private final SubmitProjectUseCase submitProjectUseCase;
    private final UpdatePartQuotasUseCase updatePartQuotasUseCase;
    private final PublishProjectUseCase publishProjectUseCase;
    private final AddProjectMemberUseCase addProjectMemberUseCase;
    private final DummyProjectFactory dummyProjectFactory;
    private final DummyApplicationFormFactory dummyApplicationFormFactory;
    private final ScenarioPartQuotaPolicy scenarioPartQuotaPolicy;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SeedProjectScenariosResult seed(SeedProjectScenariosCommand command) {
        Long gisuId = getGisuUseCase.getActiveGisuId();
        long startedAt = System.currentTimeMillis();
        log.info(
            "project scenario seed start: targetStatus={}, projectCount={}, gisuId={}",
            command.targetStatus(), command.projectCount(), gisuId
        );

        List<Long> poIds = resolvePOs(command, gisuId);

        List<CreatedProject> created = new ArrayList<>();
        List<FailedProject> failed = new ArrayList<>();

        int seq = 1;
        for (Long poMemberId : poIds) {
            ScenarioOutcome outcome = runScenario(poMemberId, command.targetStatus(), gisuId, seq);
            if (outcome.failure != null) {
                failed.add(outcome.failure);
            } else {
                created.add(outcome.success);
            }
            seq++;
        }

        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info(
            "project scenario seed completed in {}ms: created={}, failed={}",
            elapsedMs, created.size(), failed.size()
        );

        return new SeedProjectScenariosResult(created, failed);
    }

    private List<Long> resolvePOs(SeedProjectScenariosCommand command, Long gisuId) {
        List<Long> input = command.productOwnerMemberIds();
        int count = command.projectCount();

        if (input != null) {
            if (input.size() != count) {
                throw new IllegalArgumentException(
                    "productOwnerMemberIds size (%d) must equal projectCount (%d)"
                        .formatted(input.size(), count)
                );
            }
            validateAllArePlanChallengers(input, gisuId);
            return List.copyOf(input);
        }

        List<Long> pool = getChallengerUseCase.getAllByGisuId(gisuId).stream()
            .filter(c -> c.part() == ChallengerPart.PLAN)
            .filter(c -> c.challengerStatus() == ChallengerStatus.ACTIVE)
            .map(ChallengerInfo::memberId)
            .toList();
        if (pool.size() < count) {
            throw new IllegalArgumentException(
                "active PLAN challenger pool size (%d) is less than projectCount (%d)"
                    .formatted(pool.size(), count)
            );
        }
        List<Long> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return List.copyOf(shuffled.subList(0, count));
    }

    private void validateAllArePlanChallengers(List<Long> memberIds, Long gisuId) {
        Map<Long, ChallengerInfo> map = getChallengerUseCase.listByMemberIdsAndGisuId(
            new HashSet<>(memberIds), gisuId
        );
        for (Long memberId : memberIds) {
            ChallengerInfo c = map.get(memberId);
            if (c == null) {
                throw new IllegalArgumentException(
                    "memberId=%d is not a challenger of gisuId=%d".formatted(memberId, gisuId)
                );
            }
            if (c.part() != ChallengerPart.PLAN) {
                throw new IllegalArgumentException(
                    "memberId=%d is not a PLAN challenger (actual=%s)".formatted(memberId, c.part())
                );
            }
        }
    }

    private ScenarioOutcome runScenario(Long poMemberId, TargetProjectStatus target, Long gisuId, int seq) {
        Long projectId;
        try {
            projectId = createDraftProjectUseCase.create(CreateDraftProjectCommand.builder()
                .gisuId(gisuId)
                .productOwnerMemberId(poMemberId)
                .requesterMemberId(poMemberId)
                .build());
        } catch (Exception e) {
            return ScenarioOutcome.failure(failureOf(null, null, target, "CREATE_DRAFT", e));
        }
        TargetProjectStatus reached = TargetProjectStatus.DRAFT;

        try {
            updateProjectUseCase.update(UpdateProjectCommand.builder()
                .projectId(projectId)
                .requesterMemberId(poMemberId)
                .name(dummyProjectFactory.nextName(seq))
                .description(dummyProjectFactory.nextDescription(seq))
                .build());
        } catch (Exception e) {
            return ScenarioOutcome.failure(failureOf(projectId, reached, target, "UPDATE", e));
        }

        MemberInfo poMember = getMemberUseCase.getById(poMemberId);
        Long schoolId = poMember.schoolId();
        ChapterInfo chapter = getChapterUseCase.byGisuAndSchool(gisuId, schoolId);
        Long chapterId = chapter.id();

        if (target == TargetProjectStatus.DRAFT) {
            return ScenarioOutcome.success(new CreatedProject(
                projectId, TargetProjectStatus.DRAFT, poMemberId, chapterId, schoolId, null, null
            ));
        }

        Long applicationFormId;
        try {
            ApplicationFormInfo formInfo = upsertProjectApplicationFormUseCase.upsert(
                UpsertApplicationFormCommand.builder()
                    .projectId(projectId)
                    .requesterMemberId(poMemberId)
                    .title(null)
                    .description("시딩 더미 지원서")
                    .sections(dummyApplicationFormFactory.defaultSections())
                    .build()
            );
            applicationFormId = formInfo.applicationFormId();
        } catch (Exception e) {
            return ScenarioOutcome.failure(failureOf(projectId, reached, target, "FORM", e));
        }

        try {
            submitProjectUseCase.submit(SubmitProjectCommand.builder()
                .projectId(projectId)
                .build());
        } catch (Exception e) {
            return ScenarioOutcome.failure(failureOf(projectId, reached, target, "SUBMIT", e));
        }
        reached = TargetProjectStatus.PENDING_REVIEW;

        if (target == TargetProjectStatus.PENDING_REVIEW) {
            return ScenarioOutcome.success(new CreatedProject(
                projectId, TargetProjectStatus.PENDING_REVIEW, poMemberId, chapterId, schoolId,
                applicationFormId, null
            ));
        }

        List<Entry> quotas = scenarioPartQuotaPolicy.pickQuotas();
        try {
            updatePartQuotasUseCase.update(UpdatePartQuotasCommand.builder()
                .projectId(projectId)
                .entries(quotas)
                .requesterMemberId(poMemberId)
                .build());
        } catch (Exception e) {
            return ScenarioOutcome.failure(failureOf(projectId, reached, target, "QUOTA", e));
        }

        try {
            publishProjectUseCase.publish(PublishProjectCommand.builder()
                .projectId(projectId)
                .requesterMemberId(poMemberId)
                .build());
        } catch (Exception e) {
            return ScenarioOutcome.failure(failureOf(projectId, reached, target, "PUBLISH", e));
        }
        reached = TargetProjectStatus.IN_PROGRESS;

        List<PartFill> partFills = fillMembers(projectId, schoolId, gisuId, poMemberId, quotas);
        return ScenarioOutcome.success(new CreatedProject(
            projectId, TargetProjectStatus.IN_PROGRESS, poMemberId, chapterId, schoolId,
            applicationFormId, partFills
        ));
    }

    /**
     * 각 quota entry 마다 {@code target = random(0, quota)} 만큼 PO 학교의 해당 파트 ACTIVE 챌린저
     * 풀에서 무작위 추출해 add 한다. 풀이 부족하거나 개별 add 가 실패해도 best-effort 로 진행하고
     * 실제 추가된 수만 {@code filled} 에 반영한다.
     */
    private List<PartFill> fillMembers(Long projectId, Long schoolId, Long gisuId,
                                       Long poMemberId, List<Entry> quotas) {
        List<ChallengerInfo> allChallengers = getChallengerUseCase.getAllByGisuId(gisuId).stream()
            .filter(c -> c.challengerStatus() == ChallengerStatus.ACTIVE)
            .filter(c -> !Objects.equals(c.memberId(), poMemberId))
            .toList();

        Set<Long> memberIds = allChallengers.stream()
            .map(ChallengerInfo::memberId)
            .collect(Collectors.toSet());
        Map<Long, Long> memberSchoolMap = memberIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllSchoolIdsByIds(memberIds);

        Map<ChallengerPart, List<Long>> poolByPart = allChallengers.stream()
            .filter(c -> Objects.equals(memberSchoolMap.get(c.memberId()), schoolId))
            .collect(Collectors.groupingBy(
                ChallengerInfo::part,
                Collectors.mapping(ChallengerInfo::memberId, Collectors.toList())
            ));

        Set<Long> used = new HashSet<>();
        List<PartFill> fills = new ArrayList<>();
        for (Entry quota : quotas) {
            long target = nextFillCount(quota.quota());
            List<Long> available = new ArrayList<>(
                poolByPart.getOrDefault(quota.part(), List.of()).stream()
                    .filter(id -> !used.contains(id))
                    .toList()
            );
            Collections.shuffle(available);
            long actualTarget = Math.min(target, available.size());
            List<Long> picked = available.subList(0, (int) actualTarget);

            long added = 0;
            for (Long memberId : picked) {
                try {
                    addProjectMemberUseCase.add(AddProjectMemberCommand.builder()
                        .projectId(projectId)
                        .memberId(memberId)
                        .part(quota.part())
                        .requesterMemberId(poMemberId)
                        .build());
                    used.add(memberId);
                    added++;
                } catch (Exception e) {
                    log.error(
                        "project scenario seed addMember failed (projectId={}, memberId={}, part={}): {}",
                        projectId, memberId, quota.part(), e.toString()
                    );
                }
            }
            fills.add(new PartFill(quota.part(), quota.quota(), added));
        }
        return fills;
    }

    long nextFillCount(long quota) {
        return ThreadLocalRandom.current().nextLong(0, quota + 1);
    }

    private FailedProject failureOf(Long projectId, TargetProjectStatus reached,
                                    TargetProjectStatus intended, String step, Exception cause) {
        log.error(
            "project scenario seed step failed (step={}, projectId={}, reached={}, intended={}): {}",
            step, projectId, reached, intended, cause.toString()
        );
        return new FailedProject(projectId, reached, intended, step, cause.toString());
    }

    private record ScenarioOutcome(CreatedProject success, FailedProject failure) {
        static ScenarioOutcome success(CreatedProject p) {
            return new ScenarioOutcome(p, null);
        }

        static ScenarioOutcome failure(FailedProject f) {
            return new ScenarioOutcome(null, f);
        }
    }
}
