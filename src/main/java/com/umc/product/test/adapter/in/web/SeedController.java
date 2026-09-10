package com.umc.product.test.adapter.in.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.annotation.Public;
import com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRequest;
import com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerResponse;
import com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRoleRequest;
import com.umc.product.test.adapter.in.web.dto.CreateSeedChallengerRoleResponse;
import com.umc.product.test.adapter.in.web.dto.CreateSeedMemberRequest;
import com.umc.product.test.adapter.in.web.dto.CreateSeedMemberResponse;
import com.umc.product.test.adapter.in.web.dto.CreateSeedMemberSystemRoleRequest;
import com.umc.product.test.adapter.in.web.dto.CreateSeedMemberSystemRoleResponse;
import com.umc.product.test.adapter.in.web.dto.DeleteSeedProjectDataRequest;
import com.umc.product.test.adapter.in.web.dto.DeleteSeedProjectDataResponse;
import com.umc.product.test.adapter.in.web.dto.SeedChallengerPointsRequest;
import com.umc.product.test.adapter.in.web.dto.SeedChallengerPointsResponse;
import com.umc.product.test.adapter.in.web.dto.SeedChallengersRequest;
import com.umc.product.test.adapter.in.web.dto.SeedChallengersResponse;
import com.umc.product.test.adapter.in.web.dto.SeedCurriculumRequest;
import com.umc.product.test.adapter.in.web.dto.SeedCurriculumResponse;
import com.umc.product.test.adapter.in.web.dto.SeedMembersRequest;
import com.umc.product.test.adapter.in.web.dto.SeedMembersResponse;
import com.umc.product.test.adapter.in.web.dto.SeedNoticeRequest;
import com.umc.product.test.adapter.in.web.dto.SeedNoticeResponse;
import com.umc.product.test.adapter.in.web.dto.SeedProjectApplicationsRequest;
import com.umc.product.test.adapter.in.web.dto.SeedProjectApplicationsResponse;
import com.umc.product.test.adapter.in.web.dto.SeedProjectScenariosRequest;
import com.umc.product.test.adapter.in.web.dto.SeedProjectScenariosResponse;
import com.umc.product.test.adapter.in.web.dto.SeedProjectsRequest;
import com.umc.product.test.adapter.in.web.dto.SeedProjectsResponse;
import com.umc.product.test.application.port.in.command.CreateSeedChallengerRoleUseCase;
import com.umc.product.test.application.port.in.command.CreateSeedChallengerUseCase;
import com.umc.product.test.application.port.in.command.CreateSeedMemberSystemRoleUseCase;
import com.umc.product.test.application.port.in.command.CreateSeedMemberUseCase;
import com.umc.product.test.application.port.in.command.DeleteSeedProjectDataUseCase;
import com.umc.product.test.application.port.in.command.SeedChallengerPointsUseCase;
import com.umc.product.test.application.port.in.command.SeedChallengersUseCase;
import com.umc.product.test.application.port.in.command.SeedCurriculumUseCase;
import com.umc.product.test.application.port.in.command.SeedMembersUseCase;
import com.umc.product.test.application.port.in.command.SeedNoticeUseCase;
import com.umc.product.test.application.port.in.command.SeedProjectApplicationsUseCase;
import com.umc.product.test.application.port.in.command.SeedProjectScenariosUseCase;
import com.umc.product.test.application.port.in.command.SeedProjectsUseCase;
import com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * test 도메인 시딩 API. ADR-017 참조.
 * <p>
 *
 * @Profile("!prod") + app.seed.enabled=true 두 조건을 모두 만족할 때만 빈으로 등록된다. prod 환경에서는 빈 등록 자체가 차단되어 외부 노출 가능성이 없다.
 */
@RestController
@RequestMapping("/test/seed")
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Public
@Tag(name = "Test | 시딩", description = "운영 외 환경에서 시딩 데이터를 만듭니다. prod 환경에서는 비활성화됩니다.")
public class SeedController {

    private final SeedMembersUseCase seedMembersUseCase;
    private final CreateSeedMemberUseCase createSeedMemberUseCase;
    private final CreateSeedMemberSystemRoleUseCase createSeedMemberSystemRoleUseCase;
    private final SeedChallengersUseCase seedChallengersUseCase;
    private final CreateSeedChallengerUseCase createSeedChallengerUseCase;
    private final CreateSeedChallengerRoleUseCase createSeedChallengerRoleUseCase;
    private final SeedProjectsUseCase seedProjectsUseCase;
    private final DeleteSeedProjectDataUseCase deleteSeedProjectDataUseCase;
    private final SeedProjectScenariosUseCase seedProjectScenariosUseCase;
    private final SeedProjectApplicationsUseCase seedProjectApplicationsUseCase;
    private final SeedCurriculumUseCase seedCurriculumUseCase;
    private final SeedNoticeUseCase seedNoticeUseCase;
    private final SeedChallengerPointsUseCase seedChallengerPointsUseCase;

    @Operation(
        operationId = "SEED-001",
        summary = "더미 멤버 시딩",
        description = """
            ID/PW 더미 멤버를 N 명 즉시 생성합니다. 모든 더미 회원은 동일한 비밀번호
            (app.seed.default-password)를 사용합니다.
            force=false (기본) 이면 현재 회원 수가 임계값을 초과한 경우 시딩을 스킵합니다.
            force=true 이면 임계값 체크를 무시하고 무조건 시딩합니다.
            챌린저/프로젝트 시딩 전 호출이 필요합니다.
            """
    )
    @PostMapping("/members")
    public SeedMembersResponse seedMembers(@RequestBody @Valid SeedMembersRequest request) {
        return SeedMembersResponse.from(seedMembersUseCase.seed(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-001-M",
        summary = "테스트 멤버 단건 생성",
        description = """
            이름, 닉네임, 학교 ID, 이메일을 받아 ID/PW 멤버를 1명 생성합니다.
            rawPassword 를 생략하거나 공백으로 보내면 app.seed.default-password 를 사용합니다.
            활성 필수 약관은 모두 동의한 것으로 자동 처리합니다.
            """
    )
    @PostMapping("/member")
    public CreateSeedMemberResponse createMember(@RequestBody @Valid CreateSeedMemberRequest request) {
        return CreateSeedMemberResponse.from(createSeedMemberUseCase.create(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-001-R",
        summary = "테스트 회원 시스템 역할 부여",
        description = """
            회원 ID와 시스템 역할을 받아 비운영 환경의 테스트 회원에게 역할을 부여합니다.
            현재 지원되는 역할은 SUPER_ADMIN이며, 같은 역할이 이미 있으면 새 행을 만들지 않습니다.
            """
    )
    @PostMapping("/member-system-role")
    public CreateSeedMemberSystemRoleResponse createMemberSystemRole(
        @RequestBody @Valid CreateSeedMemberSystemRoleRequest request
    ) {
        return CreateSeedMemberSystemRoleResponse.from(
            createSeedMemberSystemRoleUseCase.create(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-002",
        summary = "챌린저 분포 시딩",
        description = """
            특정 기수의 학교별로 더미 회원과 챌린저를 함께 생성합니다.
            PART 기수는 parts와 countPerPartPerSchool을 사용하며, parts 생략 시 ADMIN 제외 전 파트가 대상입니다.
            TRACK 기수는 tracks와 countPerTrackPerSchool을 사용하며, tracks 생략 시 기본 4개 트랙이 대상입니다.
            각 Track 셀의 회원은 해당 Track 하나를 수강합니다. PLUS는 지원하지 않습니다.
            gisuId 생략 시 활성 기수, chapterIds 생략 시 해당 기수의 모든 지부가 대상입니다.
            학습 유형과 입력이 맞지 않으면 회원 생성 전에 거부합니다.
            """
    )
    @PostMapping("/challengers")
    public SeedChallengersResponse seedChallengers(@RequestBody @Valid SeedChallengersRequest request) {
        return SeedChallengersResponse.from(seedChallengersUseCase.seed(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-002-C",
        summary = "테스트 챌린저 단건 생성",
        description = """
            memberId와 gisuId로 챌린저 1명을 만듭니다.
            PART 기수는 part, TRACK 기수는 복수 기본 tracks를 입력합니다. PLUS는 지원하지 않습니다.
            수강 없는 운영진은 TRACK 기수에 tracks를 빈 배열로 입력한 뒤 challenger-role API로 역할을 부여합니다.
            기수 소속 생성만으로 운영진 권한이 부여되지는 않습니다.
            """
    )
    @PostMapping("/challenger")
    public CreateSeedChallengerResponse createChallenger(
        @RequestBody @Valid CreateSeedChallengerRequest request
    ) {
        return CreateSeedChallengerResponse.from(createSeedChallengerUseCase.create(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-002-R",
        summary = "테스트 챌린저 역할 단건 생성",
        description = """
            challengerId, roleType, gisuId 를 받아 운영진 역할을 1개 부여합니다.
            중앙 운영진 역할은 organizationId 없이 생성할 수 있고,
            CHAPTER_PRESIDENT 는 organizationId 에 chapterId, SCHOOL_PRESIDENT 등 학교 역할은
            organizationId 에 schoolId 를 전달합니다.
            """
    )
    @PostMapping("/challenger-role")
    public CreateSeedChallengerRoleResponse createChallengerRole(
        @RequestBody @Valid CreateSeedChallengerRoleRequest request
    ) {
        return CreateSeedChallengerRoleResponse.from(createSeedChallengerRoleUseCase.create(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-003",
        summary = "프로젝트 시딩",
        description = """
            활성 기수(또는 지정 기수)의 같은 school 멤버 풀에서 PLAN 1 + 프론트엔드 5~6 +
            백엔드 5~6 의 멤버 슬롯을 추출해 프로젝트를 N 개 생성합니다.
            School 단위 round-robin 으로 채우며, 풀이 부족한 셀은 skip 합니다.
            PO 후보가 PLAN 챌린저로 등록되지 않은 경우 시딩 측에서 PLAN 챌린저로 등록한 뒤
            createDraft 를 호출합니다.
            """
    )
    @PostMapping("/projects")
    public SeedProjectsResponse seedProjects(@RequestBody @Valid SeedProjectsRequest request) {
        return SeedProjectsResponse.from(seedProjectsUseCase.seed(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-003-D",
        summary = "프로젝트 시딩 데이터 삭제",
        description = """
            특정 기수의 프로젝트 관련 데이터를 물리 삭제합니다.
            삭제 범위는 Project, ProjectMember, ProjectPartQuota, ProjectApplication,
            ProjectApplicationForm/Policy, 해당 기수 Chapter 의 ProjectMatchingRound,
            그리고 프로젝트 지원 폼이 생성한 form Form/FormSection/Question/QuestionOption/
            FormResponse/Answer/AnswerChoice/legacy SingleAnswer 입니다.
            gisuId 가 null 이면 활성 기수를 대상으로 합니다. prod 환경에서는 노출되지 않습니다.
            """
    )
    @DeleteMapping("/projects")
    public DeleteSeedProjectDataResponse deleteProjectData(
        @RequestBody(required = false) @Valid DeleteSeedProjectDataRequest request
    ) {
        DeleteSeedProjectDataCommand command = request == null
            ? DeleteSeedProjectDataCommand.of(null)
            : request.toCommand();
        return DeleteSeedProjectDataResponse.from(deleteSeedProjectDataUseCase.delete(command));
    }

    @Operation(
        operationId = "SEED-003-S",
        summary = "프로젝트 시나리오 시딩",
        description = """
            활성 기수에 대해 DRAFT / PENDING_REVIEW / IN_PROGRESS 중 하나의 상태까지 도달한
            프로젝트를 N 개 생성합니다. SQL 직접 주입이 아니라 도메인 UseCase 시퀀스 호출로 만들기 때문에
            도메인 가드를 모두 통과한 데이터가 됩니다.
            productOwnerMemberIds 를 명시하면 그 리스트로만 PO 를 사용하고(size 가 projectCount 와
            같아야 하며 각각 활성 기수 PLAN 챌린저여야 함), null 이면 활성 기수 PLAN 챌린저 풀에서
            랜덤 픽 합니다. 시딩 측에서 챌린저를 강제로 만들지는 않습니다.
            IN_PROGRESS 단계에서는 DESIGN×1~2 + FE 1 개×3~4 + BE 1 개×3~4 의 partQuota 가 자동 분배되고,
            각 파트마다 PO 학교의 해당 파트 챌린저 풀에서 random(0, quota) 만큼 멤버가 충원됩니다.
            """
    )
    @PostMapping("/projects/scenarios")
    public SeedProjectScenariosResponse seedProjectScenarios(
        @RequestBody @Valid SeedProjectScenariosRequest request
    ) {
        return SeedProjectScenariosResponse.from(seedProjectScenariosUseCase.seed(request.toCommand()));
    }


    @Operation(
        operationId = "SEED-004",
        summary = "Curriculum 시딩 (Curriculum · WeeklyCurriculum · OriginalWorkbook · Mission)",
        description = """
            활성 기수(또는 지정 기수)의 학습 유형에 맞춰 다음 골격을 시딩합니다.
            PART 기수는 parts(생략 시 ADMIN 제외 전 파트), TRACK 기수는 tracks(생략 시 기본 4개 트랙)를 사용합니다.
            Curriculum (1/파트 또는 트랙) → WeeklyCurriculum (1~N 주차) → OriginalWorkbook (MAIN, READY) → Mission (M개).
            PLUS와 기수 학습 유형에 맞지 않는 입력은 생성 전에 거부합니다.
            releaseRequesterMemberId 가 지정되면 모든 워크북을 READY → RELEASED 로 전환합니다.
            스터디 그룹 배정이나 개인 워크북 수령은 포함하지 않습니다.
            각 단계별로 실패 격리되며, 단계별 실패 카운트가 응답에 포함됩니다.
            """
    )
    @PostMapping("/curriculum")
    public SeedCurriculumResponse seedCurriculum(@RequestBody @Valid SeedCurriculumRequest request) {
        return SeedCurriculumResponse.from(seedCurriculumUseCase.seed(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-005",
        summary = "Notice 시딩 (지부 · 학교 · 파트 분포)",
        description = """
            활성 기수(또는 지정 기수)에 대해 다음 4 가지 scope 로 공지를 분포 시딩합니다.
              GLOBAL  : 기수 전체 대상 (작성자에게 중앙 총괄단 권한 필요)
              CHAPTER : 각 지부별        (작성자에게 해당 지부 회장 권한 필요)
              SCHOOL  : 각 학교별        (작성자에게 해당 학교 회장단 권한 필요)
              PART    : 각 파트별        (작성자에게 중앙 운영진 권한 필요)
            각 공지의 제목과 내용에는 대상 범위 정보 ([전체]/[지부]/[학교]/[파트] + 식별자) 가
            포함되어 운영 화면에서 시딩 데이터 식별이 쉬워집니다.
            authorMemberId 의 권한이 부족한 scope 는 scopeBreakdown 의 failed 카운트에 잡힙니다.
            """
    )
    @PostMapping("/notice")
    public SeedNoticeResponse seedNotice(@RequestBody @Valid SeedNoticeRequest request) {
        return SeedNoticeResponse.from(seedNoticeUseCase.seed(request.toCommand()));
    }

    @Operation(
        operationId = "SEED-006",
        summary = "지원서 시나리오 시딩",
        description = """
            지정 매칭 차수 + 지부를 기준으로, 아직 팀에 합류하지 않은 ACTIVE 챌린저들이
            지부의 IN_PROGRESS 프로젝트에 지원서를 제출하는 시나리오를 실행합니다.

            전제 조건:
            - 매칭차수가 현재 OPEN 상태(startsAt <= now <= endsAt)여야 합니다.
            - 지부 내 IN_PROGRESS 프로젝트가 존재해야 합니다. (SEED-003-S 선행 필요)
            - 챌린저가 시딩되어 있어야 합니다. (SEED-002 선행 필요)

            동작:
            각 챌린저는 createDraft → fill → submit 까지 진행한 뒤, 최종 상태가
            SUBMITTED / APPROVED / REJECTED 중 하나로 무작위 결정됩니다 (약 1/3 분포).
            그 결과로 운영 화면의 "검토 대기 + 합격자 + 불합격자" 분포가 자연스럽게 채워집니다.

            ProjectMember 등록은 시딩 책임 밖입니다. 매칭 완료(APPROVED → ProjectMember 일괄 등록)는
            차수 종료 시점의 autoDecide 가 처리합니다.
            """
    )
    @PostMapping("/project-applications")
    public SeedProjectApplicationsResponse seedProjectApplications(
        @RequestBody @Valid SeedProjectApplicationsRequest request
    ) {
        return SeedProjectApplicationsResponse.from(
            seedProjectApplicationsUseCase.seed(request.toCommand())
        );
    }

    @Operation(
        operationId = "SEED-007",
        summary = "챌린저 상벌점 벌크 시딩",
        description = """
            챌린저 ID 목록을 받아 각 챌린저에게 countPerChallenger 건씩 상벌점을 부여합니다.
            타입은 상점 3종 + 벌점 2종(BLOG_CHALLENGE, PEER_REVIEW_SUBMISSION, BEST_WORKBOOK_V2,
            STUDY_LATE, NO_WORKBOOK_MISSION)을 순환하며 섞어 부여해 홈 화면 challengerHistory 에
            상점/벌점이 함께 보이도록 만듭니다. 대상 챌린저는 활동 중이거나 수료 상태여야 합니다.
            부여 방식은 단일 트랜잭션 벌크(grantChallengerPointBulk)라 건당 API 호출보다 빠릅니다.
            """
    )
    @PostMapping("/challenger-points")
    public SeedChallengerPointsResponse seedChallengerPoints(
        @RequestBody @Valid SeedChallengerPointsRequest request
    ) {
        return SeedChallengerPointsResponse.from(seedChallengerPointsUseCase.seed(request.toCommand()));
    }
}
