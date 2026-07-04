package com.umc.product.project.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.web.dto.common.MatchingRoundPhaseView;
import com.umc.product.project.adapter.in.web.dto.response.MyProjectApplicationResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicantResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicationDetailResponse;
import com.umc.product.project.application.port.in.query.GetMyProjectApplicationsUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.GetRandomMatchedProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectApplicationsUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.GetMyProjectApplicationsQuery;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ManagedProjectApplicationCardStatus;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationViewStatus;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMatchingRoundInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.domain.enums.FormResponseStatus;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationResponseAssemblerTest {

    private static final Long GISU_ID = 10L;

    @Mock
    GetMyProjectApplicationsUseCase getMyProjectApplicationsUseCase;
    @Mock
    GetRandomMatchedProjectMemberUseCase getRandomMatchedProjectMemberUseCase;
    @Mock
    SearchProjectApplicationsUseCase searchProjectApplicationsUseCase;
    @Mock
    GetProjectApplicationDetailUseCase getProjectApplicationDetailUseCase;
    @Mock
    GetProjectUseCase getProjectUseCase;
    @Mock
    GetProjectMatchingRoundUseCase getProjectMatchingRoundUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @InjectMocks
    ProjectApplicationResponseAssembler sut;

    // ============================================================
    //          myApplicationsFor (본인 지원 내역 카드 합성) 테스트
    // ============================================================

    @Test
    @DisplayName("myApplicationsFor_PM_정보가_닉네임_실명_학교명을_포함해_응답에_조립된다")
    void PM_정보_조립() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        ProjectApplicationSummaryInfo application = applicationSummaryOf(55L, 1L, 7L, 100L);
        ProjectInfo project = projectInfoOf(1L, "프로젝트A", 99L);
        ProjectMatchingRoundInfo round = roundInfoOf(7L, MatchingPhase.FIRST);

        given(getMyProjectApplicationsUseCase.listMyApplications(query))
            .willReturn(List.of(application));
        given(getRandomMatchedProjectMemberUseCase.findRandomMatched(query.requesterMemberId(), query.gisuId()))
            .willReturn(Optional.empty());
        given(getProjectUseCase.findAllByIds(any())).willReturn(Map.of(1L, project));
        given(getProjectMatchingRoundUseCase.findAllByIds(any())).willReturn(Map.of(7L, round));
        given(getMemberUseCase.findAllByIds(any()))
            .willReturn(Map.of(99L, memberOf(99L, "이방토", "이예원", "한양대 ERICA")));

        // when
        List<MyProjectApplicationResponse> result = sut.myApplicationsFor(query);

        // then
        assertThat(result).hasSize(1);
        MyProjectApplicationResponse response = result.get(0);
        assertThat(response.applicationId()).isEqualTo(55L);
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.project().productOwner().nickname()).isEqualTo("이방토");
        assertThat(response.project().productOwner().name()).isEqualTo("이예원");
        assertThat(response.project().productOwner().schoolName()).isEqualTo("한양대 ERICA");
        assertThat(response.matchingRound().id()).isEqualTo(7L);
        assertThat(response.matchingRound().type()).isEqualTo(MatchingType.PLAN_DEVELOPER);
        assertThat(response.matchingRound().phase()).isEqualTo(MatchingRoundPhaseView.FIRST);
        assertThat(response.status()).isEqualTo(ProjectApplicationViewStatus.SUBMITTED);
        assertThat(response.project().partQuotas())
            .extracting("part", "quota", "currentCount", "status")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    ChallengerPart.WEB, 3L, 1L, PartQuotaStatus.RECRUITING)
            );
    }

    @Test
    @DisplayName("myApplicationsFor_application도_randomMatched도_없으면_member_조회_없이_빈_리스트")
    void 빈_리스트_반환() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        given(getMyProjectApplicationsUseCase.listMyApplications(query))
            .willReturn(List.of());
        given(getRandomMatchedProjectMemberUseCase.findRandomMatched(query.requesterMemberId(), query.gisuId()))
            .willReturn(Optional.empty());

        // when
        List<MyProjectApplicationResponse> result = sut.myApplicationsFor(query);

        // then
        assertThat(result).isEmpty();
        verify(getMemberUseCase, never()).findAllByIds(anySet());
    }

    @Test
    @DisplayName("myApplicationsFor_RANDOM_MATCHING_카드도_PM_닉네임_실명_학교명을_동일하게_합성한다")
    void RANDOM_MATCHING_카드_PM_정보_합성() {
        // given - application 0건, randomMatched 1건
        GetMyProjectApplicationsQuery query = queryOf(null);
        ProjectInfo project = projectInfoOf(2L, "프로젝트B", 88L);
        ProjectMemberInfo member = projectMemberInfoOf(401L, 2L, ChallengerPart.WEB);

        given(getMyProjectApplicationsUseCase.listMyApplications(query))
            .willReturn(List.of());
        given(getRandomMatchedProjectMemberUseCase.findRandomMatched(query.requesterMemberId(), query.gisuId()))
            .willReturn(Optional.of(member));
        given(getProjectUseCase.findAllByIds(any())).willReturn(Map.of(2L, project));
        given(getProjectMatchingRoundUseCase.findAllByIds(any())).willReturn(Map.of());
        given(getMemberUseCase.findAllByIds(any()))
            .willReturn(Map.of(88L, memberOf(88L, "랜덤피엠", "박서은", "이화여대")));

        // when
        List<MyProjectApplicationResponse> result = sut.myApplicationsFor(query);

        // then
        assertThat(result).hasSize(1);
        MyProjectApplicationResponse response = result.get(0);
        assertThat(response.applicationId()).isNull();
        assertThat(response.projectId()).isEqualTo(2L);
        assertThat(response.matchingRound().id()).isNull();
        assertThat(response.matchingRound().type()).isEqualTo(MatchingType.PLAN_DEVELOPER);
        assertThat(response.matchingRound().phase()).isEqualTo(MatchingRoundPhaseView.RANDOM_MATCHING);
        assertThat(response.status()).isEqualTo(ProjectApplicationViewStatus.APPROVED);
        assertThat(response.project().productOwner().nickname()).isEqualTo("랜덤피엠");
        assertThat(response.project().productOwner().name()).isEqualTo("박서은");
        assertThat(response.project().productOwner().schoolName()).isEqualTo("이화여대");
    }

    @Test
    @DisplayName("myApplicationsFor_application_2건_randomMatched_1건이면_RANDOM_MATCHING_카드는_마지막에_append")
    void 랜덤매칭_카드_끝에_append() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        ProjectApplicationSummaryInfo appA = applicationSummaryOf(55L, 1L, 7L, 100L);
        ProjectApplicationSummaryInfo appB = applicationSummaryOf(56L, 2L, 8L, 100L);
        ProjectMemberInfo member = projectMemberInfoOf(401L, 3L, ChallengerPart.WEB);

        ProjectInfo pA = projectInfoOf(1L, "프로젝트A", 99L);
        ProjectInfo pB = projectInfoOf(2L, "프로젝트B", 99L);
        ProjectInfo pC = projectInfoOf(3L, "프로젝트C", 88L);

        given(getMyProjectApplicationsUseCase.listMyApplications(query))
            .willReturn(List.of(appA, appB));
        given(getRandomMatchedProjectMemberUseCase.findRandomMatched(query.requesterMemberId(), query.gisuId()))
            .willReturn(Optional.of(member));
        given(getProjectUseCase.findAllByIds(any()))
            .willReturn(Map.of(1L, pA, 2L, pB, 3L, pC));
        given(getProjectMatchingRoundUseCase.findAllByIds(any())).willReturn(Map.of(
            7L, roundInfoOf(7L, MatchingPhase.FIRST),
            8L, roundInfoOf(8L, MatchingPhase.SECOND)
        ));
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of());

        // when
        List<MyProjectApplicationResponse> result = sut.myApplicationsFor(query);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(MyProjectApplicationResponse::projectId)
            .containsExactly(1L, 2L, 3L);
        assertThat(result.get(0).matchingRound().phase()).isEqualTo(MatchingRoundPhaseView.FIRST);
        assertThat(result.get(1).matchingRound().phase()).isEqualTo(MatchingRoundPhaseView.SECOND);
        MyProjectApplicationResponse last = result.get(2);
        assertThat(last.applicationId()).isNull();
        assertThat(last.matchingRound().id()).isNull();
        assertThat(last.matchingRound().phase()).isEqualTo(MatchingRoundPhaseView.RANDOM_MATCHING);
        assertThat(last.status()).isEqualTo(ProjectApplicationViewStatus.APPROVED);
    }

    @Test
    @DisplayName("myApplicationsFor_status_명시_호출에서는_랜덤매칭_조회를_생략한다")
    void status_명시시_랜덤매칭_조회_생략() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(ProjectApplicationStatus.SUBMITTED);
        given(getMyProjectApplicationsUseCase.listMyApplications(query))
            .willReturn(List.of());

        // when
        List<MyProjectApplicationResponse> result = sut.myApplicationsFor(query);

        // then
        assertThat(result).isEmpty();
        verify(getRandomMatchedProjectMemberUseCase, never())
            .findRandomMatched(any(), any());
    }

    @Test
    @DisplayName("myApplicationsFor_PM_정보_조회_결과가_없으면_productOwner는_null")
    void PM_정보_없음_null() {
        // given
        GetMyProjectApplicationsQuery query = queryOf(null);
        ProjectApplicationSummaryInfo application = applicationSummaryOf(55L, 1L, 7L, 100L);
        ProjectInfo project = projectInfoOf(1L, "프로젝트A", 99L);

        given(getMyProjectApplicationsUseCase.listMyApplications(query))
            .willReturn(List.of(application));
        given(getRandomMatchedProjectMemberUseCase.findRandomMatched(query.requesterMemberId(), query.gisuId()))
            .willReturn(Optional.empty());
        given(getProjectUseCase.findAllByIds(any())).willReturn(Map.of(1L, project));
        given(getProjectMatchingRoundUseCase.findAllByIds(any()))
            .willReturn(Map.of(7L, roundInfoOf(7L, MatchingPhase.FIRST)));
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of());

        // when
        List<MyProjectApplicationResponse> result = sut.myApplicationsFor(query);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).project().productOwner()).isNull();
    }

    // ============================================================
    //          applicantsFor (PM/운영진 지원자 목록) 테스트
    // ============================================================

    @Test
    @DisplayName("applicantsFor_지원자_member_정보가_닉네임_실명_학교명을_포함해_응답에_조립된다")
    void applicantsFor_정상_조립() {
        // given
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(100L)
            .projectId(1L).build();
        ProjectApplicationSummaryInfo application = applicationSummaryOf(55L, 1L, 7L, 200L);
        ProjectInfo project = projectInfoOf(1L, "프로젝트A", 99L);

        given(searchProjectApplicationsUseCase.searchByProject(query))
            .willReturn(List.of(application));
        given(getProjectUseCase.getById(1L)).willReturn(project);
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(eq(Set.of(200L)), eq(GISU_ID)))
            .willReturn(Map.of(200L, challengerInfoOf(200L, ChallengerPart.WEB)));
        given(getProjectMatchingRoundUseCase.findAllByIds(any()))
            .willReturn(Map.of(7L, roundInfoOf(7L, MatchingPhase.FIRST)));
        given(getMemberUseCase.findAllByIds(any()))
            .willReturn(Map.of(200L, memberOf(200L, "벨라", "황지원", "중앙대")));

        // when
        List<ProjectApplicantResponse> result = sut.applicantsFor(query);

        // then
        assertThat(result).hasSize(1);
        ProjectApplicantResponse response = result.get(0);
        assertThat(response.applicationId()).isEqualTo(55L);
        assertThat(response.applicant().memberId()).isEqualTo(200L);
        assertThat(response.applicant().nickname()).isEqualTo("벨라");
        assertThat(response.applicant().name()).isEqualTo("황지원");
        assertThat(response.applicant().schoolName()).isEqualTo("중앙대");
        assertThat(response.applicant().part()).isEqualTo(ChallengerPart.WEB);
        assertThat(response.matchingRound().id()).isEqualTo(7L);
        assertThat(response.matchingRound().type()).isEqualTo(MatchingType.PLAN_DEVELOPER);
        assertThat(response.matchingRound().phase()).isEqualTo(MatchingRoundPhaseView.FIRST);
        assertThat(response.status()).isEqualTo(ManagedProjectApplicationCardStatus.SUBMITTED);
    }

    @Test
    @DisplayName("applicantsFor_지원자가_없으면_member_조회_없이_빈_리스트")
    void applicantsFor_빈_리스트() {
        // given
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(100L)
            .projectId(1L).build();
        given(searchProjectApplicationsUseCase.searchByProject(query))
            .willReturn(List.of());

        // when
        List<ProjectApplicantResponse> result = sut.applicantsFor(query);

        // then
        assertThat(result).isEmpty();
        verify(getMemberUseCase, never()).findAllByIds(anySet());
        verify(getChallengerUseCase, never()).batchGetByMemberIdsAndGisuId(any(), any());
    }

    @Test
    @DisplayName("applicantsFor_member_조회_누락_시_applicant_의_닉네임_실명_학교명은_null")
    void applicantsFor_member_누락_시_null() {
        // given
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(100L)
            .projectId(1L).build();
        ProjectApplicationSummaryInfo application = applicationSummaryOf(55L, 1L, 7L, 200L);
        ProjectInfo project = projectInfoOf(1L, "프로젝트A", 99L);

        given(searchProjectApplicationsUseCase.searchByProject(query))
            .willReturn(List.of(application));
        given(getProjectUseCase.getById(1L)).willReturn(project);
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), eq(GISU_ID)))
            .willReturn(Map.of(200L, challengerInfoOf(200L, ChallengerPart.WEB)));
        given(getProjectMatchingRoundUseCase.findAllByIds(any()))
            .willReturn(Map.of(7L, roundInfoOf(7L, MatchingPhase.FIRST)));
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of());

        // when
        List<ProjectApplicantResponse> result = sut.applicantsFor(query);

        // then
        assertThat(result).hasSize(1);
        ProjectApplicantResponse response = result.get(0);
        assertThat(response.applicant().memberId()).isEqualTo(200L);
        assertThat(response.applicant().nickname()).isNull();
        assertThat(response.applicant().name()).isNull();
        assertThat(response.applicant().schoolName()).isNull();
        // part 는 challenger 도메인 -> Assembler 가 batch 조회해 채운다 (member 누락과 무관)
        assertThat(response.applicant().part()).isEqualTo(ChallengerPart.WEB);
    }

    @Test
    @DisplayName("applicantsFor_part_필터를_지정하면_해당_파트_지원자만_반환")
    void applicantsFor_part_필터() {
        // given - WEB / ANDROID 지원자가 1명씩 있는데 query.part=WEB 으로 필터링
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(100L)
            .projectId(1L)
            .part(ChallengerPart.WEB)
            .build();
        ProjectApplicationSummaryInfo webApp = applicationSummaryOf(55L, 1L, 7L, 200L);
        ProjectApplicationSummaryInfo androidApp = applicationSummaryOf(56L, 1L, 7L, 201L);
        ProjectInfo project = projectInfoOf(1L, "프로젝트A", 99L);

        given(searchProjectApplicationsUseCase.searchByProject(query))
            .willReturn(List.of(webApp, androidApp));
        given(getProjectUseCase.getById(1L)).willReturn(project);
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), eq(GISU_ID)))
            .willReturn(Map.of(
                200L, challengerInfoOf(200L, ChallengerPart.WEB),
                201L, challengerInfoOf(201L, ChallengerPart.ANDROID)
            ));
        given(getProjectMatchingRoundUseCase.findAllByIds(any()))
            .willReturn(Map.of(7L, roundInfoOf(7L, MatchingPhase.FIRST)));
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of());

        // when
        List<ProjectApplicantResponse> result = sut.applicantsFor(query);

        // then - WEB 1건만
        assertThat(result).hasSize(1);
        assertThat(result.get(0).applicant().memberId()).isEqualTo(200L);
        assertThat(result.get(0).applicant().part()).isEqualTo(ChallengerPart.WEB);
    }

    @Test
    @DisplayName("applicantsFor_차수_파트_시간_순으로_정렬되어_반환된다")
    void applicantsFor_정렬_순서() {
        // given - 입력은 일부러 셔플된 순서.
        // 차수(phase) ASC -> 파트(ChallengerPart#sortOrder) ASC -> 제출 시각(submittedAt) ASC 로 정렬되어야 한다.
        // FIRST/SECOND/THIRD 세 차수, DESIGN(1)/WEB(2)/ANDROID(3)/IOS(4)/NODEJS(5) 다섯 파트, 같은 (차수,파트) 묶음에서 시간이 ASC 인지까지 검증.
        Long roundFirstId = 11L;
        Long roundSecondId = 22L;
        Long roundThirdId = 33L;
        Instant t06 = Instant.parse("2026-04-22T06:00:00Z");
        Instant t07 = Instant.parse("2026-04-22T07:00:00Z");
        Instant t08 = Instant.parse("2026-04-22T08:00:00Z");
        Instant t09 = Instant.parse("2026-04-22T09:00:00Z");
        Instant t10 = Instant.parse("2026-04-22T10:00:00Z");
        Instant t11 = Instant.parse("2026-04-22T11:00:00Z");

        ProjectApplicationSummaryInfo a = applicationSummaryOf(101L, 1L, roundSecondId, 300L, t10); // SECOND / DESIGN
        ProjectApplicationSummaryInfo b = applicationSummaryOf(102L, 1L, roundFirstId, 301L, t09);  // FIRST / WEB
        ProjectApplicationSummaryInfo c = applicationSummaryOf(103L, 1L, roundFirstId, 302L, t11);  // FIRST / DESIGN
        ProjectApplicationSummaryInfo d = applicationSummaryOf(104L, 1L, roundFirstId, 303L, t08);  // FIRST / ANDROID
        ProjectApplicationSummaryInfo e = applicationSummaryOf(105L, 1L, roundSecondId, 304L, t09); // SECOND / NODEJS
        ProjectApplicationSummaryInfo f = applicationSummaryOf(106L, 1L, roundFirstId, 305L, t07);  // FIRST / WEB
        ProjectApplicationSummaryInfo g = applicationSummaryOf(107L, 1L, roundThirdId, 306L, t06);  // THIRD / IOS
        ProjectApplicationSummaryInfo h = applicationSummaryOf(108L, 1L, roundSecondId, 307L, t09); // SECOND / DESIGN

        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(100L)
            .projectId(1L)
            .build();
        ProjectInfo project = projectInfoOf(1L, "프로젝트A", 99L);

        given(searchProjectApplicationsUseCase.searchByProject(query))
            .willReturn(List.of(a, b, c, d, e, f, g, h));
        given(getProjectUseCase.getById(1L)).willReturn(project);
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(any(), eq(GISU_ID)))
            .willReturn(Map.of(
                300L, challengerInfoOf(300L, ChallengerPart.DESIGN),
                301L, challengerInfoOf(301L, ChallengerPart.WEB),
                302L, challengerInfoOf(302L, ChallengerPart.DESIGN),
                303L, challengerInfoOf(303L, ChallengerPart.ANDROID),
                304L, challengerInfoOf(304L, ChallengerPart.NODEJS),
                305L, challengerInfoOf(305L, ChallengerPart.WEB),
                306L, challengerInfoOf(306L, ChallengerPart.IOS),
                307L, challengerInfoOf(307L, ChallengerPart.DESIGN)
            ));
        given(getProjectMatchingRoundUseCase.findAllByIds(any()))
            .willReturn(Map.of(
                roundFirstId, roundInfoOf(roundFirstId, MatchingPhase.FIRST),
                roundSecondId, roundInfoOf(roundSecondId, MatchingPhase.SECOND),
                roundThirdId, roundInfoOf(roundThirdId, MatchingPhase.THIRD)
            ));
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of());

        // when
        List<ProjectApplicantResponse> result = sut.applicantsFor(query);

        // then - 기대 순서:
        //   FIRST  / DESIGN(1)   / 11:00  -> c (103)
        //   FIRST  / WEB(2)      / 07:00  -> f (106)
        //   FIRST  / WEB(2)      / 09:00  -> b (102)
        //   FIRST  / ANDROID(3)  / 08:00  -> d (104)
        //   SECOND / DESIGN(1)   / 09:00  -> h (108)
        //   SECOND / DESIGN(1)   / 10:00  -> a (101)
        //   SECOND / NODEJS(5)   / 09:00  -> e (105)
        //   THIRD  / IOS(4)      / 06:00  -> g (107)   // 시간이 가장 빨라도 차수가 가장 늦으면 마지막
        assertThat(result).extracting(ProjectApplicantResponse::applicationId)
            .containsExactly(103L, 106L, 102L, 104L, 108L, 101L, 105L, 107L);
    }

    @Test
    @DisplayName("applicantsForBatch_프로젝트별로_응답을_묶고_part_필터를_적용한다")
    void applicantsForBatch_프로젝트별_그룹핑_및_part_필터() {
        // given
        SearchProjectApplicationsBatchQuery query = SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(100L)
            .projectIds(List.of(1L, 2L, 3L))
            .part(ChallengerPart.WEB)
            .build();
        ProjectApplicationSummaryInfo webApp = applicationSummaryOf(55L, 1L, 7L, 200L);
        ProjectApplicationSummaryInfo androidApp = applicationSummaryOf(56L, 1L, 7L, 201L);
        ProjectApplicationSummaryInfo secondProjectApp = applicationSummaryOf(57L, 2L, 8L, 202L);

        Map<Long, List<ProjectApplicationSummaryInfo>> applicationsByProject = new LinkedHashMap<>();
        applicationsByProject.put(1L, List.of(androidApp, webApp));
        applicationsByProject.put(2L, List.of(secondProjectApp));
        applicationsByProject.put(3L, List.of());

        given(searchProjectApplicationsUseCase.searchByProjects(query))
            .willReturn(applicationsByProject);
        given(getProjectUseCase.findAllByIds(eq(Set.of(1L, 2L))))
            .willReturn(Map.of(
                1L, projectInfoOf(1L, "프로젝트A", 99L),
                2L, projectInfoOf(2L, "프로젝트B", 88L)
            ));
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(eq(Set.of(200L, 201L, 202L)), eq(GISU_ID)))
            .willReturn(Map.of(
                200L, challengerInfoOf(200L, ChallengerPart.WEB),
                201L, challengerInfoOf(201L, ChallengerPart.ANDROID),
                202L, challengerInfoOf(202L, ChallengerPart.WEB)
            ));
        given(getProjectMatchingRoundUseCase.findAllByIds(eq(Set.of(7L, 8L))))
            .willReturn(Map.of(
                7L, roundInfoOf(7L, MatchingPhase.FIRST),
                8L, roundInfoOf(8L, MatchingPhase.SECOND)
            ));
        given(getMemberUseCase.findAllByIds(eq(Set.of(200L, 201L, 202L))))
            .willReturn(Map.of(
                200L, memberOf(200L, "웹지원자", "김웹", "중앙대"),
                202L, memberOf(202L, "웹지원자2", "이웹", "숭실대")
            ));

        // when
        Map<Long, List<ProjectApplicantResponse>> result = sut.applicantsForBatch(query);

        // then
        assertThat(result.keySet()).containsExactly(1L, 2L, 3L);
        assertThat(result.get(1L)).extracting(ProjectApplicantResponse::applicationId)
            .containsExactly(55L);
        assertThat(result.get(1L).get(0).applicant().part()).isEqualTo(ChallengerPart.WEB);
        assertThat(result.get(1L).get(0).applicant().nickname()).isEqualTo("웹지원자");
        assertThat(result.get(2L)).extracting(ProjectApplicantResponse::applicationId)
            .containsExactly(57L);
        assertThat(result.get(3L)).isEmpty();
    }

    @Test
    @DisplayName("applicantsForBatch_프로젝트별_차수_파트_시간_순으로_정렬되어_반환된다")
    void applicantsForBatch_정렬_순서() {
        // given
        Long roundFirstId = 11L;
        Long roundSecondId = 22L;
        ProjectApplicationSummaryInfo secondDesignLate = applicationSummaryOf(
            101L, 1L, roundSecondId, 300L, Instant.parse("2026-04-22T10:00:00Z"));
        ProjectApplicationSummaryInfo firstWebLate = applicationSummaryOf(
            102L, 1L, roundFirstId, 301L, Instant.parse("2026-04-22T09:00:00Z"));
        ProjectApplicationSummaryInfo firstDesign = applicationSummaryOf(
            103L, 1L, roundFirstId, 302L, Instant.parse("2026-04-22T11:00:00Z"));
        ProjectApplicationSummaryInfo firstWebEarly = applicationSummaryOf(
            104L, 1L, roundFirstId, 303L, Instant.parse("2026-04-22T07:00:00Z"));

        SearchProjectApplicationsBatchQuery query = SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(100L)
            .projectIds(List.of(1L))
            .build();
        Map<Long, List<ProjectApplicationSummaryInfo>> applicationsByProject = new LinkedHashMap<>();
        applicationsByProject.put(1L, List.of(secondDesignLate, firstWebLate, firstDesign, firstWebEarly));

        given(searchProjectApplicationsUseCase.searchByProjects(query))
            .willReturn(applicationsByProject);
        given(getProjectUseCase.findAllByIds(eq(Set.of(1L))))
            .willReturn(Map.of(1L, projectInfoOf(1L, "프로젝트A", 99L)));
        given(getChallengerUseCase.batchGetByMemberIdsAndGisuId(eq(Set.of(300L, 301L, 302L, 303L)), eq(GISU_ID)))
            .willReturn(Map.of(
                300L, challengerInfoOf(300L, ChallengerPart.DESIGN),
                301L, challengerInfoOf(301L, ChallengerPart.WEB),
                302L, challengerInfoOf(302L, ChallengerPart.DESIGN),
                303L, challengerInfoOf(303L, ChallengerPart.WEB)
            ));
        given(getProjectMatchingRoundUseCase.findAllByIds(eq(Set.of(roundFirstId, roundSecondId))))
            .willReturn(Map.of(
                roundFirstId, roundInfoOf(roundFirstId, MatchingPhase.FIRST),
                roundSecondId, roundInfoOf(roundSecondId, MatchingPhase.SECOND)
            ));
        given(getMemberUseCase.findAllByIds(eq(Set.of(300L, 301L, 302L, 303L))))
            .willReturn(Map.of());

        // when
        Map<Long, List<ProjectApplicantResponse>> result = sut.applicantsForBatch(query);

        // then
        assertThat(result.get(1L)).extracting(ProjectApplicantResponse::applicationId)
            .containsExactly(103L, 104L, 102L, 101L);
    }

    // ============================================================
    //                   detailFor (단건 상세) 테스트
    // ============================================================

    @Test
    @DisplayName("detailFor_지원자_member_정보가_닉네임_실명_학교명을_포함해_응답에_조립된다")
    void detailFor_정상_조립() {
        // given
        GetProjectApplicationDetailQuery query = detailQueryOf(1L, 55L);
        ProjectApplicationDetailInfo info = detailInfoOf(55L, 200L, ChallengerPart.DESIGN);

        given(getProjectApplicationDetailUseCase.getDetail(query)).willReturn(info);
        given(getMemberUseCase.findAllByIds(any()))
            .willReturn(Map.of(200L, memberOf(200L, "이방토", "이예원", "한양대 ERICA")));

        // when
        ProjectApplicationDetailResponse response = sut.detailFor(query);

        // then
        assertThat(response.applicationId()).isEqualTo(55L);
        assertThat(response.applicant().memberId()).isEqualTo(200L);
        assertThat(response.applicant().nickname()).isEqualTo("이방토");
        assertThat(response.applicant().name()).isEqualTo("이예원");
        assertThat(response.applicant().schoolName()).isEqualTo("한양대 ERICA");
        assertThat(response.applicant().part()).isEqualTo(ChallengerPart.DESIGN);
        assertThat(response.matchingRound().id()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(ProjectApplicationViewStatus.SUBMITTED);
        assertThat(response.formResponse().formResponseId()).isEqualTo(123L);
        assertThat(response.formResponse().sections()).isEmpty();
    }

    @Test
    @DisplayName("detailFor_member_조회_누락_시_applicant_의_닉네임_실명_학교명은_null")
    void detailFor_member_누락_시_null() {
        // given
        GetProjectApplicationDetailQuery query = detailQueryOf(1L, 55L);
        ProjectApplicationDetailInfo info = detailInfoOf(55L, 200L, ChallengerPart.DESIGN);

        given(getProjectApplicationDetailUseCase.getDetail(query)).willReturn(info);
        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of());

        // when
        ProjectApplicationDetailResponse response = sut.detailFor(query);

        // then
        assertThat(response.applicant().memberId()).isEqualTo(200L);
        assertThat(response.applicant().nickname()).isNull();
        assertThat(response.applicant().name()).isNull();
        assertThat(response.applicant().schoolName()).isNull();
        // part 는 challenger 도메인 -- Service 단에서 채워서 옴 (member 누락과 무관)
        assertThat(response.applicant().part()).isEqualTo(ChallengerPart.DESIGN);
    }

    // ============================================================
    //                      Helper Methods
    // ============================================================

    private GetMyProjectApplicationsQuery queryOf(ProjectApplicationStatus status) {
        return GetMyProjectApplicationsQuery.builder()
            .requesterMemberId(100L)
            .gisuId(GISU_ID)
            .status(status)
            .build();
    }

    private ProjectApplicationSummaryInfo applicationSummaryOf(
        Long applicationId, Long projectId, Long matchingRoundId, Long applicantMemberId
    ) {
        return applicationSummaryOf(
            applicationId, projectId, matchingRoundId, applicantMemberId,
            Instant.parse("2026-04-22T01:30:00Z")
        );
    }

    private ProjectApplicationSummaryInfo applicationSummaryOf(
        Long applicationId, Long projectId, Long matchingRoundId, Long applicantMemberId, Instant submittedAt
    ) {
        return ProjectApplicationSummaryInfo.builder()
            .id(applicationId)
            .applicantMemberId(applicantMemberId)
            .applicationFormId(33L)
            .projectId(projectId)
            .matchingRoundId(matchingRoundId)
            .status(ProjectApplicationStatus.SUBMITTED)
            .submittedAt(submittedAt)
            .build();
    }

    private ProjectInfo projectInfoOf(Long projectId, String name, Long productOwnerMemberId) {
        return ProjectInfo.builder()
            .id(projectId)
            .status(ProjectStatus.IN_PROGRESS)
            .name(name)
            .gisuId(GISU_ID)
            .thumbnailImageUrl("https://cdn.example.com/thumb-" + projectId)
            .productOwnerMemberId(productOwnerMemberId)
            .coProductOwnerMemberIds(List.of())
            .partQuotas(List.of(ProjectPartQuotaInfo.of(ChallengerPart.WEB, 3L, 1L)))
            .build();
    }

    private ProjectMatchingRoundInfo roundInfoOf(Long id, MatchingPhase phase) {
        return ProjectMatchingRoundInfo.builder()
            .id(id)
            .type(MatchingType.PLAN_DEVELOPER)
            .phase(phase)
            .build();
    }

    private ProjectMemberInfo projectMemberInfoOf(Long memberId, Long projectId, ChallengerPart part) {
        return ProjectMemberInfo.builder()
            .projectMemberId(memberId)
            .projectId(projectId)
            .memberId(100L)
            .part(part)
            .isLeader(false)
            .status(ProjectMemberStatus.ACTIVE)
            .build();
    }

    private MemberInfo memberOf(Long id, String nickname, String name, String schoolName) {
        return MemberInfo.builder()
            .id(id)
            .nickname(nickname)
            .name(name)
            .schoolName(schoolName)
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private ChallengerInfo challengerInfoOf(Long memberId, ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(memberId * 10)
            .memberId(memberId)
            .gisuId(GISU_ID)
            .part(part)
            .build();
    }

    private GetProjectApplicationDetailQuery detailQueryOf(Long projectId, Long applicationId) {
        return GetProjectApplicationDetailQuery.builder()
            .projectId(projectId)
            .applicationId(applicationId)
            .requesterMemberId(100L)
            .build();
    }

    private ProjectApplicationDetailInfo detailInfoOf(
        Long applicationId, Long applicantMemberId, ChallengerPart part
    ) {
        ApplicationFormInfo emptyForm = ApplicationFormInfo.builder()
            .projectId(1L)
            .applicationFormId(33L)
            .title("지원폼")
            .description(null)
            .sections(List.of())
            .build();
        FormResponseInfo formResponse = FormResponseInfo.builder()
            .id(123L)
            .formId(7L)
            .respondentMemberId(applicantMemberId)
            .status(FormResponseStatus.SUBMITTED)
            .submittedAt(Instant.parse("2026-04-22T01:30:00Z"))
            .lastSavedAt(Instant.parse("2026-04-22T01:30:00Z"))
            .build();

        return ProjectApplicationDetailInfo.builder()
            .applicationId(applicationId)
            .applicantMemberId(applicantMemberId)
            .applicantPart(part)
            .matchingRoundId(7L)
            .matchingRoundType(MatchingType.PLAN_DESIGN)
            .matchingRoundPhase(MatchingPhase.FIRST)
            .status(ProjectApplicationViewStatus.SUBMITTED)
            .submittedAt(Instant.parse("2026-04-22T01:30:00Z"))
            .statusChangedAt(null)
            .formStructure(emptyForm)
            .formResponse(formResponse)
            .answersByQuestionId(Map.of())
            .filesByFileId(Map.of())
            .build();
    }
}
