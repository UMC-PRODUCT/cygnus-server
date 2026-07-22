package com.umc.product.project.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo.SelectedOption;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.graphql.dto.MemberBriefGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectPageGraphQlRequest;
import com.umc.product.project.adapter.in.graphql.dto.ProjectPageGraphQlRequest.ProjectSort;
import com.umc.product.project.adapter.in.graphql.dto.ProjectPageGraphQlResponse;
import com.umc.product.project.adapter.in.web.dto.common.ApplicationFormSection;
import com.umc.product.project.adapter.in.web.dto.common.ApplicationQuestionItem;
import com.umc.product.project.adapter.in.web.dto.common.ApplicationQuestionOptionItem;
import com.umc.product.project.adapter.in.web.dto.common.MatchingRoundPhaseView;
import com.umc.product.project.adapter.in.web.dto.common.MemberBrief;
import com.umc.product.project.adapter.in.web.dto.request.AddProjectMemberRequest;
import com.umc.product.project.adapter.in.web.dto.request.CreateDraftProjectRequest;
import com.umc.product.project.adapter.in.web.dto.request.CreateProjectApplicationRequest;
import com.umc.product.project.adapter.in.web.dto.request.TransferProjectOwnershipRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdatePartQuotasRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateProjectMatchingRoundRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateProjectRequest;
import com.umc.product.project.adapter.in.web.dto.response.ManagedProjectSummaryResponse;
import com.umc.product.project.adapter.in.web.dto.response.MyProjectApplicationResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicationDetailResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectDetailResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectStatusResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectSummaryResponse;
import com.umc.product.project.application.access.ProjectApplicationAccessScope;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectApplicationCommand;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.ManagedProjectApplicationCardStatus;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;

@DisplayName("Project adapter DTO 잔여 변환")
class ProjectDtoResidualTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("요청 DTO는 path·인증 정보와 payload를 command로 손실 없이 변환한다")
    void request_dtos_map_to_commands() {
        assertThat(new AddProjectMemberRequest(2L, ChallengerPart.DESIGN).toCommand(1L, 3L))
            .satisfies(command -> {
                assertThat(command.projectId()).isEqualTo(1L);
                assertThat(command.memberId()).isEqualTo(2L);
                assertThat(command.requesterMemberId()).isEqualTo(3L);
            });
        assertThat(new TransferProjectOwnershipRequest(2L, "양도").toCommand(1L).reason())
            .isEqualTo("양도");
        assertThat(new CreateProjectApplicationRequest(4L).toCommand(1L, 2L).matchingRoundId())
            .isEqualTo(4L);

        assertThat(new CreateDraftProjectRequest(5L, null).toCommand(9L).productOwnerMemberId())
            .isEqualTo(9L);
        assertThat(new CreateDraftProjectRequest(5L, 8L).toCommand(9L).productOwnerMemberId())
            .isEqualTo(8L);

        var update = new UpdateProjectRequest("이름", "설명", "link", "thumb", "logo")
            .toCommand(1L, 2L);
        assertThat(update.projectId()).isEqualTo(1L);
        assertThat(update.logoFileId()).isEqualTo("logo");

        var quotas = new UpdatePartQuotasRequest(List.of(
            new UpdatePartQuotasRequest.Entry(ChallengerPart.WEB, 3L)))
            .toCommand(1L, 2L);
        assertThat(quotas.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.part()).isEqualTo(ChallengerPart.WEB);
            assertThat(entry.quota()).isEqualTo(3L);
        });
        assertThat(CreateDraftProjectApplicationCommand.builder()
            .projectId(1L).applicantMemberId(2L).matchingRoundId(3L).build().projectId())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("지원서 access scope record는 각 범위 값을 보존한다")
    void application_access_scope_records_preserve_values() {
        assertThat(new ProjectApplicationAccessScope.OwnerOnly(1L).memberId()).isEqualTo(1L);
        assertThat(new ProjectApplicationAccessScope.ChapterScoped(List.of(2L), 3L).chapterIds())
            .containsExactly(2L);
        assertThat(new ProjectApplicationAccessScope.AllInGisu(4L).gisuId()).isEqualTo(4L);
        assertThat(new ProjectApplicationAccessScope.All()).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 요약은 빈 TO와 모집 중·완료 TO를 올바르게 집계한다")
    void summary_aggregates_quota_status() {
        MemberBrief owner = new MemberBrief(2L, "nick", "name", "school");
        ProjectSummaryResponse empty = ProjectSummaryResponse.from(projectInfo(List.of()), owner);
        ProjectSummaryResponse recruiting = ProjectSummaryResponse.from(projectInfo(List.of(
            ProjectPartQuotaInfo.of(ChallengerPart.WEB, 2L, 1L),
            ProjectPartQuotaInfo.of(ChallengerPart.DESIGN, 1L, 1L))), owner);
        ProjectSummaryResponse completed = ProjectSummaryResponse.from(projectInfo(List.of(
            ProjectPartQuotaInfo.of(ChallengerPart.WEB, 1L, 1L))), owner);

        assertThat(empty.partQuotaStatus()).isNull();
        assertThat(recruiting.partQuotaStatus()).isEqualTo(PartQuotaStatus.RECRUITING);
        assertThat(completed.partQuotaStatus()).isEqualTo(PartQuotaStatus.COMPLETED);
        assertThat(ProjectStatusResponse.of(1L, ProjectStatus.DRAFT).projectId()).isEqualTo(1L);
        assertThat(ProjectStatusResponse.from(projectInfo(List.of())).status()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Member·Page GraphQL DTO는 enrichment와 paging metadata를 보존한다")
    void member_and_page_graphql_responses() {
        MemberInfo member = MemberInfo.builder()
            .id(1L).nickname("nick").name("name").schoolName("school")
            .status(MemberStatus.ACTIVE).build();
        MemberBriefGraphQlResponse brief = MemberBriefGraphQlResponse.from(member);
        var page = new PageImpl<>(List.of(projectInfo(List.of())), PageRequest.of(1, 1), 3L);
        ProjectPageGraphQlResponse response = ProjectPageGraphQlResponse.from(page);

        assertThat(brief.memberId()).isEqualTo(1L);
        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("지원서 상세 Web·GraphQL 변환은 선택지 snapshot·파일 누락·시간을 안전하게 처리한다")
    void application_detail_conversions_preserve_nested_data() {
        ProjectApplicationDetailInfo info = detailInfo();

        ProjectApplicationDetailResponse web = ProjectApplicationDetailResponse.from(info, null);
        ProjectApplicationGraphQlResponse graphQl = ProjectApplicationGraphQlResponse.from(info);

        assertThat(web.applicant().nickname()).isNull();
        assertThat(web.formResponse().sections()).singleElement()
            .extracting(section -> section.questions().get(0).answer().files().size())
            .isEqualTo(1);
        assertThat(graphQl.formResponse().sections()).singleElement()
            .extracting(section -> section.questions().get(0).answer().times())
            .isEqualTo(List.of(NOW.toString()));
        assertThat(graphQl.formResponse().sections().get(0).questions().get(0).answer().selectedOptions())
            .singleElement()
            .extracting(ProjectApplicationGraphQlResponse.ProjectApplicationSelectedOptionGraphQlResponse::answeredAsContent)
            .isEqualTo("선택 snapshot");
    }

    @Test
    @DisplayName("지원서 GraphQL 변환은 null 구조·round·answer collection을 빈 값으로 fail-safe 처리한다")
    void application_graphql_null_boundaries() {
        ProjectApplicationDetailInfo missingRound = ProjectApplicationDetailInfo.builder()
            .applicationId(1L).applicantMemberId(2L).applicantPart(ChallengerPart.WEB)
            .status(ProjectApplicationViewStatus.DRAFT).build();

        ProjectApplicationGraphQlResponse response = ProjectApplicationGraphQlResponse.from(missingRound);
        assertThat(response.matchingRound()).isNull();
        assertThat(response.formResponse()).isNull();
        assertThat(ProjectApplicationGraphQlResponse.ProjectApplicationFormResponseGraphQlResponse.from(
            null, formStructure(), null, null)).isNull();

        AnswerInfo empty = AnswerInfo.builder()
            .id(1L).answeredAsType(QuestionType.FILE)
            .selectedOptions(List.of()).fileIds(null).times(null).build();
        var answer = ProjectApplicationGraphQlResponse.ProjectApplicationAnswerGraphQlResponse.from(
            empty, Map.of());
        assertThat(answer.files()).isEmpty();
        assertThat(answer.times()).isEmpty();
    }

    @Test
    @DisplayName("지원 폼 Web DTO는 section-question-option 구조를 양방향 변환한다")
    void application_form_web_dtos_convert_nested_structure() {
        ApplicationFormInfo.SectionInfo sectionInfo = formStructure().sections().get(0);

        ApplicationFormSection section = ApplicationFormSection.from(sectionInfo);
        ApplicationQuestionItem question = ApplicationQuestionItem.from(sectionInfo.questions().get(0));
        ApplicationQuestionOptionItem option = ApplicationQuestionOptionItem.from(
            sectionInfo.questions().get(0).options().get(0));

        assertThat(section.toEntry().questions()).hasSize(1);
        assertThat(question.toEntry().options()).hasSize(1);
        assertThat(option.toEntry().content()).isEqualTo("선택");
    }

    @Test
    @DisplayName("상태 표시 enum은 모든 도메인 상태를 명시적으로 변환한다")
    void view_status_enums_cover_all_domain_values() {
        assertThat(ProjectApplicationViewStatus.from(ProjectApplicationStatus.DRAFT))
            .isEqualTo(ProjectApplicationViewStatus.DRAFT);
        assertThat(ProjectApplicationViewStatus.from(ProjectApplicationStatus.SUBMITTED))
            .isEqualTo(ProjectApplicationViewStatus.SUBMITTED);
        assertThat(ProjectApplicationViewStatus.from(ProjectApplicationStatus.APPROVED))
            .isEqualTo(ProjectApplicationViewStatus.APPROVED);
        assertThat(ProjectApplicationViewStatus.from(ProjectApplicationStatus.REJECTED))
            .isEqualTo(ProjectApplicationViewStatus.REJECTED);
        assertThat(ProjectApplicationViewStatus.from(ProjectApplicationStatus.CANCELLED))
            .isEqualTo(ProjectApplicationViewStatus.CANCELLED);

        assertThat(ManagedProjectApplicationCardStatus.from(ProjectApplicationStatus.SUBMITTED))
            .isEqualTo(ManagedProjectApplicationCardStatus.SUBMITTED);
        assertThat(ManagedProjectApplicationCardStatus.from(ProjectApplicationStatus.APPROVED))
            .isEqualTo(ManagedProjectApplicationCardStatus.APPROVED);
        assertThat(ManagedProjectApplicationCardStatus.from(ProjectApplicationStatus.REJECTED))
            .isEqualTo(ManagedProjectApplicationCardStatus.REJECTED);
        assertThatThrownBy(() -> ManagedProjectApplicationCardStatus.from(ProjectApplicationStatus.DRAFT))
            .isInstanceOf(ProjectDomainException.class);
        assertThatThrownBy(() -> ManagedProjectApplicationCardStatus.from(ProjectApplicationStatus.CANCELLED))
            .isInstanceOf(ProjectDomainException.class);

        assertThat(MatchingRoundPhaseView.from(MatchingPhase.FIRST)).isEqualTo(MatchingRoundPhaseView.FIRST);
        assertThat(MatchingRoundPhaseView.from(MatchingPhase.SECOND)).isEqualTo(MatchingRoundPhaseView.SECOND);
        assertThat(MatchingRoundPhaseView.from(MatchingPhase.THIRD)).isEqualTo(MatchingRoundPhaseView.THIRD);
    }

    @Test
    @DisplayName("상세·관리 응답은 빈 TO와 모집 중·완료 TO를 각각 집계한다")
    void detail_and_managed_responses_aggregate_quota_status() {
        MemberBrief owner = new MemberBrief(2L, "nick", "name", "school");
        List<List<ProjectPartQuotaInfo>> cases = List.of(
            List.of(),
            List.of(ProjectPartQuotaInfo.of(ChallengerPart.WEB, 2L, 1L)),
            List.of(ProjectPartQuotaInfo.of(ChallengerPart.WEB, 1L, 1L))
        );

        assertThat(cases.stream()
            .map(quotas -> ProjectDetailResponse.from(projectInfo(quotas), owner, List.of(), null).partQuotaStatus()))
            .containsExactly(null, PartQuotaStatus.RECRUITING, PartQuotaStatus.COMPLETED);
        assertThat(cases.stream()
            .map(quotas -> ManagedProjectSummaryResponse.from(projectInfo(quotas), owner).partQuotaStatus()))
            .containsExactly(null, PartQuotaStatus.RECRUITING, PartQuotaStatus.COMPLETED);
    }

    @Test
    @DisplayName("지원 내역은 누락 프로젝트와 누락 라운드를 null-safe하게 변환한다")
    void my_application_response_handles_missing_project_and_round() {
        ProjectApplicationSummaryInfo application = ProjectApplicationSummaryInfo.builder()
            .id(1L).projectId(2L).matchingRoundId(3L).applicantMemberId(4L)
            .status(null).build();

        MyProjectApplicationResponse response =
            MyProjectApplicationResponse.fromApplication(application, null, null, null);

        assertThat(response.project()).isNull();
        assertThat(response.matchingRound().id()).isNull();
        assertThat(response.status()).isNull();
    }

    @Test
    @DisplayName("검색 query는 필수 상태·owner 조합과 batch ID 경계를 검증한다")
    void search_queries_validate_boundaries() {
        PageRequest pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> SearchProjectQuery.builder()
            .gisuId(1L).statuses(null).pageable(pageable).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchProjectQuery.builder()
            .gisuId(1L).statuses(List.of(ProjectStatus.IN_PROGRESS))
            .includedOwnerStatuses(List.of(ProjectStatus.DRAFT)).pageable(pageable).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchProjectQuery.builder()
            .gisuId(1L).statuses(List.of(ProjectStatus.IN_PROGRESS))
            .includedOwnerMemberId(2L).pageable(pageable).build())
            .isInstanceOf(IllegalArgumentException.class);
        SearchProjectQuery query = SearchProjectQuery.forAdmin(
            1L, null, null, null, null, null, List.of(ProjectStatus.DRAFT), pageable);
        assertThat(query.withStatuses(null).statuses()).containsExactly(ProjectStatus.IN_PROGRESS);
        assertThat(SearchProjectQuery.forAdmin(
            1L, null, null, null, null, null, null, pageable).statuses())
            .containsExactly(ProjectStatus.IN_PROGRESS);
        assertThat(SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, pageable).statuses())
            .containsExactly(ProjectStatus.IN_PROGRESS);
        assertThat(query.withChapterFilter(3L, Set.of(ProjectStatus.COMPLETED)).chapterId()).isEqualTo(3L);
        assertThat(query.withOwnerFilter(4L, Set.of(ProjectStatus.COMPLETED)).productOwnerMemberId()).isEqualTo(4L);
        assertThat(query.withIncludedOwner(5L, Set.of(ProjectStatus.DRAFT)).includedOwnerMemberId()).isEqualTo(5L);
        assertThatThrownBy(() -> query.withIncludedOwner(2L, Set.of()))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(1L).projectIds(List.of()).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(1L).projectIds(java.util.Arrays.asList(1L, null)).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(1L).projectIds(List.of(1L)).status(ProjectApplicationStatus.DRAFT).build())
            .isInstanceOf(ProjectDomainException.class);
    }

    @Test
    @DisplayName("GraphQL page와 프로젝트 응답은 경계·시간·nullable collection을 처리한다")
    void graphql_page_and_project_boundaries() {
        assertThatThrownBy(() -> new ProjectPageGraphQlRequest(-1, 20, null).toPageable())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectPageGraphQlRequest(0, 0, null).toPageable())
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ProjectPageGraphQlRequest(0, 20, List.of(ProjectSort.NAME_DESC))
            .toPageable().getSort().getOrderFor("name").isDescending()).isTrue();

        ProjectInfo info = ProjectInfo.builder()
            .id(1L).status(ProjectStatus.IN_PROGRESS).name("프로젝트")
            .coProductOwnerMemberIds(null).partQuotas(null)
            .createdAt(NOW).updatedAt(NOW).build();
        ProjectGraphQlResponse response = ProjectGraphQlResponse.from(info);
        assertThat(response.coProductOwnerMemberIds()).isEmpty();
        assertThat(response.partQuotas()).isEmpty();
        assertThat(response.createdAt()).isEqualTo(NOW.toString());
    }

    @Test
    @DisplayName("매칭 차수 수정 요청은 blank 이름과 chapterId 필드를 거부한다")
    void update_matching_round_request_rejects_invalid_fields() {
        assertThatThrownBy(() -> new UpdateProjectMatchingRoundRequest(
            " ", null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        UpdateProjectMatchingRoundRequest request = new UpdateProjectMatchingRoundRequest(
            null, null, null, null, null, null, null);
        assertThatThrownBy(() -> request.rejectUnknownProperty("chapterId", 1L))
            .isInstanceOf(IllegalArgumentException.class);
        request.rejectUnknownProperty("ignored", 1L);
    }

    @Test
    @DisplayName("Web AnswerView는 첨부 ID가 null이면 빈 파일 목록을 반환한다")
    void web_answer_view_handles_null_file_ids() {
        AnswerInfo answer = AnswerInfo.builder()
            .id(1L).answeredAsType(QuestionType.FILE)
            .selectedOptions(List.of()).fileIds(null).times(Set.of()).build();

        var response = ProjectApplicationDetailResponse.SectionView.AnswerView.of(answer, Map.of());

        assertThat(response.files()).isEmpty();
    }

    @Test
    @DisplayName("지원서 상세 factory는 중복 질문 답변 중 첫 답변을 보존한다")
    void application_detail_factory_preserves_first_duplicate_answer() {
        Project project = Project.createDraft(1L, 2L, 3L, 4L, 3L);
        ReflectionTestUtils.setField(project, "id", 10L);
        ProjectApplicationForm applicationForm = ProjectApplicationForm.create(project, 20L);
        ReflectionTestUtils.setField(applicationForm, "id", 30L);
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "1차", null, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST, 2L,
            NOW.minusSeconds(3_600), NOW.plusSeconds(3_600), NOW.plusSeconds(7_200));
        ReflectionTestUtils.setField(round, "id", 40L);
        ProjectApplication application = ProjectApplication.create(applicationForm, 50L, 6L, round);
        ReflectionTestUtils.setField(application, "id", 60L);
        ReflectionTestUtils.setField(application, "status", ProjectApplicationStatus.SUBMITTED);
        AnswerInfo first = AnswerInfo.builder()
            .id(1L).questionId(100L).answeredAsType(QuestionType.SHORT_TEXT)
            .textValue("첫 답변").selectedOptions(List.of()).build();
        AnswerInfo duplicate = AnswerInfo.builder()
            .id(2L).questionId(100L).answeredAsType(QuestionType.SHORT_TEXT)
            .textValue("중복 답변").selectedOptions(List.of()).build();
        FormResponseWithAnswersInfo response = FormResponseWithAnswersInfo.builder()
            .id(50L).formId(20L).respondentMemberId(6L).status(FormResponseStatus.SUBMITTED)
            .answers(List.of(first, duplicate)).build();
        FormWithStructureInfo structure = FormWithStructureInfo.builder()
            .formId(20L).title("지원 폼").sections(List.of()).build();

        ProjectApplicationDetailInfo result = ProjectApplicationDetailInfo.of(
            application, ChallengerPart.WEB, structure, List.of(), response, Map.of());

        assertThat(result.answersByQuestionId().get(100L).textValue()).isEqualTo("첫 답변");
    }

    private ProjectInfo projectInfo(List<ProjectPartQuotaInfo> quotas) {
        return ProjectInfo.builder()
            .id(1L).status(ProjectStatus.IN_PROGRESS).name("프로젝트").description("설명")
            .thumbnailImageUrl("thumb").productOwnerMemberId(2L).partQuotas(quotas).build();
    }

    private ProjectApplicationDetailInfo detailInfo() {
        AnswerInfo answer = AnswerInfo.builder()
            .id(30L).questionId(20L).answeredAsType(QuestionType.PORTFOLIO)
            .textValue("portfolio")
            .selectedOptions(List.of(new SelectedOption(null, "선택 snapshot")))
            .fileIds(Set.of("exists", "missing")).times(Set.of(NOW)).build();
        FileInfo file = new FileInfo(
            "exists", "portfolio.pdf", FileCategory.PORTFOLIO, "application/pdf",
            10L, "https://file", true, 1L, NOW
        );
        return ProjectApplicationDetailInfo.builder()
            .applicationId(1L).applicantMemberId(2L).applicantPart(ChallengerPart.WEB)
            .matchingRoundId(3L).matchingRoundType(MatchingType.PLAN_DEVELOPER)
            .matchingRoundPhase(MatchingPhase.FIRST)
            .status(ProjectApplicationViewStatus.SUBMITTED)
            .submittedAt(NOW).statusChangedAt(NOW)
            .formStructure(formStructure()).formResponse(formResponse())
            .answersByQuestionId(Map.of(20L, answer)).filesByFileId(Map.of("exists", file))
            .build();
    }

    private ApplicationFormInfo formStructure() {
        ApplicationFormInfo.OptionInfo option = ApplicationFormInfo.OptionInfo.builder()
            .optionId(40L).content("선택").orderNo(1L).isOther(false).build();
        ApplicationFormInfo.QuestionInfo question = ApplicationFormInfo.QuestionInfo.builder()
            .questionId(20L).type(QuestionType.PORTFOLIO).title("질문").description("설명")
            .isRequired(true).orderNo(1L).options(List.of(option)).build();
        ApplicationFormInfo.SectionInfo section = ApplicationFormInfo.SectionInfo.builder()
            .sectionId(10L).type(FormSectionType.PART).allowedParts(Set.of(ChallengerPart.WEB))
            .title("섹션").description("설명").orderNo(1L).questions(List.of(question)).build();
        return ApplicationFormInfo.builder()
            .projectId(1L).applicationFormId(2L).title("지원 폼")
            .description("설명").sections(List.of(section)).build();
    }

    private FormResponseInfo formResponse() {
        return FormResponseInfo.builder()
            .id(4L).formId(5L).status(FormResponseStatus.SUBMITTED)
            .submittedAt(NOW).lastSavedAt(NOW).build();
    }
}
