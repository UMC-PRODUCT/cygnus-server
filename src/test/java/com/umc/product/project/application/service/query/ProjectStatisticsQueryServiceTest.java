package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectStatisticsInfo;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.LoadProjectStatisticsPort;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsApplicationRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsApprovedApplicationRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsMatchingRoundRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsMemberRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsProjectRow;
import com.umc.product.project.application.service.policy.ProjectStatisticsAccessPolicy;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;

@ExtendWith(MockitoExtension.class)
class ProjectStatisticsQueryServiceTest {

    @Mock
    LoadProjectStatisticsPort loadProjectStatisticsPort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    LoadProjectPort loadProjectPort;

    @Mock
    ProjectStatisticsAccessPolicy projectStatisticsAccessPolicy;

    @InjectMocks
    ProjectStatisticsQueryService sut;

    @Test
    @DisplayName("getByProjectId_기존_멤버_지원_이력과_차수별_BFF_통계를_함께_반환한다")
    void 단건_프로젝트_BFF_통계_반환() {
        // given
        Long projectId = 10L;
        Long gisuId = 1L;
        Long chapterId = 3L;

        given(loadProjectStatisticsPort.getProjectById(projectId))
            .willReturn(projectRow(projectId, gisuId, chapterId));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId))
            .willReturn(List.of(
                roundRow(1L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                roundRow(2L, MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(loadProjectStatisticsPort.listActiveMembersByProjectId(projectId))
            .willReturn(List.of(
                memberRow(projectId, 101L, 1001L, ChallengerPart.WEB),
                memberRow(projectId, 102L, 1002L, ChallengerPart.DESIGN)
            ));
        given(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(Set.of(projectId)))
            .willReturn(List.of(
                applicationRow(projectId, 1001L, 201L, ProjectApplicationStatus.APPROVED, 1L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                applicationRow(projectId, 1003L, 202L, ProjectApplicationStatus.REJECTED, 1L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                applicationRow(projectId, 1004L, 203L, ProjectApplicationStatus.SUBMITTED, 2L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of(
                challenger(1001L, gisuId, ChallengerPart.WEB),
                challenger(1002L, gisuId, ChallengerPart.DESIGN),
                challenger(1003L, gisuId, ChallengerPart.WEB),
                challenger(1004L, gisuId, ChallengerPart.ANDROID),
                challenger(9001L, gisuId, ChallengerPart.PLAN),
                challenger(9002L, gisuId, ChallengerPart.ADMIN),
                // 수료/제명 챌린저는 지부 모집단(지원 가능 인원)에서 제외되어야 한다.
                challenger(1005L, gisuId, ChallengerPart.WEB, ChallengerStatus.GRADUATED),
                challenger(1006L, gisuId, ChallengerPart.SPRINGBOOT, ChallengerStatus.EXPELLED)
            ));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1001L, 1002L, 1003L, 1004L)))
            .willReturn(Map.of(
                1001L, 501L,
                1002L, 502L,
                1003L, 502L,
                1004L, 501L
            ));
        // 요청자가 해당 프로젝트의 PO → FULL (멤버 단위 포함)
        Long requesterMemberId = 7000L;
        given(loadProjectPort.getById(projectId))
            .willReturn(project(projectId, requesterMemberId, chapterId));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(eq(requesterMemberId), any(Project.class)))
            .willReturn(true);

        // when
        ProjectStatisticsInfo result = sut.getByProjectId(projectId, requesterMemberId);

        // then
        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.projectMembers())
            .extracting("projectMemberId", "memberId", "part", "status")
            .containsExactly(
                tuple(101L, 1001L, ChallengerPart.WEB, ProjectMemberStatus.ACTIVE),
                tuple(102L, 1002L, ChallengerPart.DESIGN, ProjectMemberStatus.ACTIVE)
            );

        assertThat(result.projectMembers().get(0).applications())
            .extracting("applicationId", "status")
            .containsExactly(
                tuple(201L, ProjectApplicationStatus.APPROVED)
            );
        assertThat(result.projectMembers().get(0).applications())
            .extracting(a -> a.matchingRound().type(), a -> a.matchingRound().phase())
            .containsExactly(
                tuple(MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST)
            );
        assertThat(result.projectMembers().get(1).applications()).isEmpty();

        assertThat(result.roundApplicationStatistics())
            .extracting(
                s -> s.matchingRound().matchingRoundId(),
                s -> s.appliedMemberCount(),
                s -> s.availableMemberCount()
            )
            .containsExactly(
                tuple(1L, 2L, 4L),
                tuple(2L, 1L, 3L)
            );
        assertThat(result.schoolApplicationStatistics().get(0).matchingRound().matchingRoundId()).isEqualTo(1L);
        assertThat(result.schoolApplicationStatistics().get(0).schools())
            .extracting("schoolId", "applicantCount")
            .containsExactly(
                tuple(501L, 1L),
                tuple(502L, 1L)
            );
        assertThat(result.schoolApplicationStatistics().get(1).matchingRound().matchingRoundId()).isEqualTo(2L);
        assertThat(result.schoolApplicationStatistics().get(1).schools())
            .extracting("schoolId", "applicantCount")
            .containsExactly(tuple(501L, 1L));
    }

    @Test
    @DisplayName("getByChapterId_프로젝트별_기존_필드와_지부_BFF_요약을_함께_반환한다")
    void 지부_BFF_통계_반환() {
        // given
        Long chapterId = 3L;
        Long gisuId = 1L;
        given(loadProjectStatisticsPort.listProjectsByChapterId(chapterId))
            .willReturn(List.of(
                projectRow(10L, gisuId, chapterId),
                projectRow(11L, gisuId, chapterId)
            ));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId))
            .willReturn(List.of(
                roundRow(1L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                roundRow(2L, MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(loadProjectStatisticsPort.listActiveMembersByChapterId(chapterId))
            .willReturn(List.of(
                memberRow(10L, 101L, 1001L, ChallengerPart.WEB),
                memberRow(10L, 102L, 1002L, ChallengerPart.DESIGN),
                memberRow(11L, 201L, 1004L, ChallengerPart.WEB)
            ));
        given(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(Set.of(10L, 11L)))
            .willReturn(List.of(
                applicationRow(10L, 1001L, 301L, ProjectApplicationStatus.APPROVED, 1L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                applicationRow(10L, 1003L, 302L, ProjectApplicationStatus.REJECTED, 1L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                applicationRow(11L, 1003L, 303L, ProjectApplicationStatus.SUBMITTED, 1L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                applicationRow(11L, 1004L, 304L, ProjectApplicationStatus.APPROVED, 2L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND),
                applicationRow(10L, 9001L, 305L, ProjectApplicationStatus.SUBMITTED, 2L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of(
                challenger(1001L, gisuId, ChallengerPart.WEB),
                challenger(1002L, gisuId, ChallengerPart.DESIGN),
                challenger(1003L, gisuId, ChallengerPart.WEB),
                challenger(1004L, gisuId, ChallengerPart.ANDROID),
                challenger(9001L, gisuId, ChallengerPart.PLAN),
                challenger(9002L, gisuId, ChallengerPart.ADMIN)
            ));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1001L, 1002L, 1003L, 1004L)))
            .willReturn(Map.of(
                1001L, 501L,
                1002L, 502L,
                1003L, 502L,
                1004L, 501L
            ));
        // 요청자가 총괄단 → FULL (멤버 단위 포함)
        Long requesterMemberId = 7000L;
        given(projectStatisticsAccessPolicy.canReadChapterStatistics(requesterMemberId, chapterId))
            .willReturn(true);

        // when
        ChapterProjectStatisticsInfo result = sut.getByChapterId(chapterId, requesterMemberId);

        // then
        assertThat(result.chapterId()).isEqualTo(chapterId);
        assertThat(result.projects())
            .extracting(ProjectStatisticsInfo::projectId)
            .containsExactly(10L, 11L);
        assertThat(result.projects().get(0).projectMembers()).hasSize(2);
        assertThat(result.projects().get(0).projectMembers().get(0).applications())
            .extracting("applicationId", "status")
            .containsExactly(tuple(301L, ProjectApplicationStatus.APPROVED));
        assertThat(result.projects().get(0).projectMembers().get(1).applications()).isEmpty();
        assertThat(result.projects().get(1).projectMembers()).hasSize(1);
        assertThat(result.projects().get(1).projectMembers().get(0).applications())
            .extracting("applicationId", "status")
            .containsExactly(tuple(304L, ProjectApplicationStatus.APPROVED));

        assertThat(result.summary().roundApplicationStatistics())
            .extracting(
                s -> s.matchingRound().matchingRoundId(),
                s -> s.appliedMemberCount(),
                s -> s.availableMemberCount()
            )
            .containsExactly(
                tuple(1L, 3L, 4L),
                tuple(2L, 1L, 3L)
            );
        assertThat(result.summary().roundSchoolRankings().get(0).schools())
            .extracting("schoolId", "applicantCount")
            .containsExactly(
                tuple(501L, 1L),
                tuple(502L, 1L)
            );
        assertThat(result.summary().schoolMatchingStatistics())
            .extracting("schoolId", "matchedMemberCount", "totalMemberCount", "appliedMemberCount")
            .containsExactly(
                tuple(501L, 2L, 2L, 2L),
                tuple(502L, 1L, 2L, 1L)
            );
        assertThat(result.summary().projectRoundStatistics())
            .extracting("projectId")
            .containsExactly(10L, 11L);
        assertThat(result.summary().projectRoundStatistics().get(0).matchingRounds())
            .extracting(
                s -> s.matchingRound().matchingRoundId(),
                s -> s.appliedMemberCount(),
                s -> s.matchedMemberCount()
            )
            .containsExactly(
                tuple(1L, 2L, 1L),
                tuple(2L, 0L, 0L)
            );
        assertThat(result.summary().projectRoundStatistics().get(1).matchingRounds())
            .extracting(
                s -> s.matchingRound().matchingRoundId(),
                s -> s.appliedMemberCount(),
                s -> s.matchedMemberCount()
            )
            .containsExactly(
                tuple(1L, 1L, 0L),
                tuple(2L, 1L, 1L)
            );

        verify(loadProjectStatisticsPort).listProjectsByChapterId(chapterId);
        verify(loadProjectStatisticsPort).listActiveMembersByChapterId(chapterId);
    }

    @Test
    @DisplayName("getByProjectIds_프로젝트_목록을_지부_응답_형태로_반환한다")
    void 프로젝트_목록_통계_반환() {
        // given
        Long chapterId = 3L;
        Long gisuId = 1L;
        Long requesterMemberId = 7000L;
        Set<Long> projectIds = Set.of(10L, 11L);
        given(loadProjectPort.listByIds(projectIds))
            .willReturn(List.of(
                project(10L, requesterMemberId, chapterId),
                project(11L, requesterMemberId, chapterId)
            ));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId))
            .willReturn(List.of(
                roundRow(1L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                roundRow(2L, MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(loadProjectStatisticsPort.listActiveMembersByProjectIds(projectIds))
            .willReturn(List.of(
                memberRow(10L, 101L, 1001L, ChallengerPart.WEB),
                memberRow(11L, 201L, 1004L, ChallengerPart.WEB)
            ));
        given(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(projectIds))
            .willReturn(List.of(
                applicationRow(10L, 1001L, 301L, ProjectApplicationStatus.APPROVED, 1L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                applicationRow(11L, 1004L, 304L, ProjectApplicationStatus.APPROVED, 2L,
                    MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of(
                challenger(1001L, gisuId, ChallengerPart.WEB),
                challenger(1002L, gisuId, ChallengerPart.DESIGN),
                challenger(1003L, gisuId, ChallengerPart.WEB),
                challenger(1004L, gisuId, ChallengerPart.ANDROID)
            ));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1001L, 1002L, 1003L, 1004L)))
            .willReturn(Map.of(
                1001L, 501L,
                1002L, 502L,
                1003L, 502L,
                1004L, 501L
            ));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(eq(requesterMemberId), any(Project.class)))
            .willReturn(true);

        // when
        ChapterProjectStatisticsInfo result = sut.getByProjectIds(List.of(10L, 11L), requesterMemberId);

        // then
        assertThat(result.chapterId()).isEqualTo(chapterId);
        assertThat(result.projects())
            .extracting(ProjectStatisticsInfo::projectId)
            .containsExactly(10L, 11L);
        assertThat(result.summary().roundApplicationStatistics())
            .extracting(
                s -> s.matchingRound().matchingRoundId(),
                s -> s.appliedMemberCount(),
                s -> s.availableMemberCount()
            )
            .containsExactly(
                tuple(1L, 1L, 4L),
                tuple(2L, 1L, 3L)
            );
        assertThat(result.summary().projectRoundStatistics())
            .extracting("projectId")
            .containsExactly(10L, 11L);

        verify(loadProjectStatisticsPort).listActiveMembersByProjectIds(projectIds);
    }

    @Test
    @DisplayName("getByChapterId_null_memberId는_학교별_summary_집계에서_무시한다")
    void 지부_통계_null_memberId_학교별_summary_집계_무시() {
        // given
        Long chapterId = 3L;
        Long gisuId = 1L;
        Long requesterMemberId = 7000L;
        given(loadProjectStatisticsPort.listProjectsByChapterId(chapterId))
            .willReturn(List.of(projectRow(10L, gisuId, chapterId)));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId))
            .willReturn(List.of(roundRow(1L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST)));
        given(loadProjectStatisticsPort.listActiveMembersByChapterId(chapterId))
            .willReturn(List.of(memberRow(10L, 101L, null, ChallengerPart.WEB)));
        given(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(Set.of(10L)))
            .willReturn(List.of());
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of());
        given(projectStatisticsAccessPolicy.canReadChapterStatistics(requesterMemberId, chapterId))
            .willReturn(true);

        // when
        ChapterProjectStatisticsInfo result = sut.getByChapterId(chapterId, requesterMemberId);

        // then
        assertThat(result.summary().schoolMatchingStatistics()).isEmpty();
    }

    @Test
    @DisplayName("getPublicMatchingStatisticsByChapterId_ProjectMember_기준_공개_매칭_요약을_반환한다")
    void 공개_프로젝트_매칭_요약_반환() {
        // given
        Long chapterId = 3L;
        Long gisuId = 1L;
        given(loadProjectStatisticsPort.listPublicProjectsByChapterId(chapterId))
            .willReturn(List.of(
                projectRow(10L, gisuId, chapterId),
                projectRow(11L, gisuId, chapterId)
            ));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId))
            .willReturn(List.of(
                roundRow(1L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                roundRow(2L, MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));
        given(loadProjectStatisticsPort.listPublicActiveMembersByChapterId(chapterId))
            .willReturn(List.of(
                memberRow(10L, 101L, 1001L, ChallengerPart.WEB),
                memberRow(10L, 102L, 1002L, ChallengerPart.DESIGN),
                memberRow(11L, 201L, 1003L, ChallengerPart.ANDROID),
                memberRow(11L, 202L, 1004L, ChallengerPart.IOS)
            ));
        given(loadProjectStatisticsPort.listApprovedApplicationsByProjectIds(Set.of(10L, 11L)))
            .willReturn(List.of(
                approvedApplicationRow(10L, 1001L, 502L, 2L, MatchingType.PLAN_DEVELOPER,
                    MatchingPhase.SECOND, "2026-03-02T00:00:00Z"),
                approvedApplicationRow(10L, 1001L, 501L, 1L, MatchingType.PLAN_DEVELOPER,
                    MatchingPhase.FIRST, "2026-03-01T00:00:00Z"),
                approvedApplicationRow(10L, 1002L, 503L, 1L, MatchingType.PLAN_DEVELOPER,
                    MatchingPhase.FIRST, "2026-03-01T00:00:00Z"),
                approvedApplicationRow(11L, 1003L, 504L, 2L, MatchingType.PLAN_DEVELOPER,
                    MatchingPhase.SECOND, "2026-03-02T00:00:00Z")
            ));
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of(
                challenger(1001L, gisuId, ChallengerPart.WEB),
                challenger(1002L, gisuId, ChallengerPart.DESIGN),
                challenger(1003L, gisuId, ChallengerPart.ANDROID),
                challenger(1004L, gisuId, ChallengerPart.IOS),
                challenger(1005L, gisuId, ChallengerPart.SPRINGBOOT),
                challenger(9001L, gisuId, ChallengerPart.PLAN),
                challenger(9002L, gisuId, ChallengerPart.ADMIN)
            ));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1001L, 1002L, 1003L, 1004L, 1005L)))
            .willReturn(Map.of(
                1001L, 501L,
                1002L, 502L,
                1003L, 501L,
                1004L, 502L,
                1005L, 502L
            ));

        // when
        ChapterProjectMatchingStatisticsInfo result = sut.getPublicMatchingStatisticsByChapterId(chapterId);

        // then
        assertThat(result.chapterId()).isEqualTo(chapterId);
        assertThat(result.roundMatchingStatistics())
            .extracting(
                s -> s.matchingRound().matchingRoundId(),
                s -> s.matchedMemberCount(),
                s -> s.availableMemberCount()
            )
            .containsExactly(
                tuple(1L, 2L, 5L),
                tuple(2L, 1L, 3L)
            );
        assertThat(result.roundMatchingStatistics().get(0).projects())
            .extracting("projectId", "matchedMemberCount")
            .containsExactly(tuple(10L, 2L));
        assertThat(result.roundMatchingStatistics().get(1).projects())
            .extracting("projectId", "matchedMemberCount")
            .containsExactly(tuple(11L, 1L));
        assertThat(result.schoolMatchingStatistics())
            .extracting("schoolId", "matchedMemberCount", "totalMemberCount")
            .containsExactly(
                tuple(501L, 2L, 2L),
                tuple(502L, 2L, 3L)
            );
        assertThat(result.unclassifiedMatchingStatistics().matchedMemberCount()).isEqualTo(1L);
        assertThat(result.unclassifiedMatchingStatistics().projects())
            .extracting("projectId", "matchedMemberCount")
            .containsExactly(tuple(11L, 1L));

        verify(loadProjectStatisticsPort).listPublicProjectsByChapterId(chapterId);
        verify(loadProjectStatisticsPort).listPublicActiveMembersByChapterId(chapterId);
    }

    @Test
    @DisplayName("getByChapterId_지부장이면_조회_가능하다")
    void 지부_지부장이면_조회_가능() {
        // given
        Long chapterId = 3L;
        Long gisuId = 1L;
        Long requesterMemberId = 8000L;
        givenSingleProjectChapter(chapterId, gisuId);
        given(projectStatisticsAccessPolicy.canReadChapterStatistics(requesterMemberId, chapterId))
            .willReturn(true);

        // when
        ChapterProjectStatisticsInfo result = sut.getByChapterId(chapterId, requesterMemberId);

        // then — 권한 통과: 프로젝트 멤버까지 그대로 노출
        assertThat(result.projects()).hasSize(1);
        assertThat(result.projects().get(0).projectMembers()).hasSize(1);
        assertThat(result.summary()).isNotNull();
    }

    @Test
    @DisplayName("getByChapterId_해당_지부_소속_학교_회장이면_조회_가능하다")
    void 지부_학교_회장이면_조회_가능() {
        // given
        Long chapterId = 3L;
        Long gisuId = 1L;
        Long schoolId = 7L;
        Long requesterMemberId = 8100L;
        givenSingleProjectChapter(chapterId, gisuId);
        given(projectStatisticsAccessPolicy.canReadChapterStatistics(requesterMemberId, chapterId))
            .willReturn(true);

        // when
        ChapterProjectStatisticsInfo result = sut.getByChapterId(chapterId, requesterMemberId);

        // then
        assertThat(result.projects()).hasSize(1);
        assertThat(result.projects().get(0).projectMembers()).hasSize(1);
    }

    @Test
    @DisplayName("getByChapterId_권한이_없으면_PROJECT_ACCESS_DENIED")
    void 지부_권한없으면_거부() {
        // given
        Long chapterId = 3L;
        Long requesterMemberId = 8200L;
        given(projectStatisticsAccessPolicy.canReadChapterStatistics(requesterMemberId, chapterId))
            .willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.getByChapterId(chapterId, requesterMemberId))
            .isInstanceOf(ProjectDomainException.class);
    }

    @Test
    @DisplayName("getByProjectId_보조PM이면_FULL로_멤버_단위를_포함한다")
    void 단건_보조PM이면_FULL() {
        // given
        Long projectId = 10L;
        Long gisuId = 1L;
        Long chapterId = 3L;
        Long requesterMemberId = 8300L;
        givenSingleProject(projectId, gisuId, chapterId);
        // PO 는 아니지만 ACTIVE PLAN 멤버(Sub-PM)
        given(loadProjectPort.getById(projectId))
            .willReturn(project(projectId, 9999L, chapterId));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(eq(requesterMemberId), any(Project.class)))
            .willReturn(true);

        // when
        ProjectStatisticsInfo result = sut.getByProjectId(projectId, requesterMemberId);

        // then
        assertThat(result.projectMembers()).hasSize(1);
    }

    @Test
    @DisplayName("getByProjectId_지부장이면_조회_가능하다")
    void 단건_지부장이면_조회_가능() {
        // given
        Long projectId = 10L;
        Long gisuId = 1L;
        Long chapterId = 3L;
        Long requesterMemberId = 8400L;
        givenSingleProject(projectId, gisuId, chapterId);
        given(loadProjectPort.getById(projectId))
            .willReturn(project(projectId, 9999L, chapterId));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(eq(requesterMemberId), any(Project.class)))
            .willReturn(true);

        // when
        ProjectStatisticsInfo result = sut.getByProjectId(projectId, requesterMemberId);

        // then — 권한 통과: 프로젝트 멤버까지 그대로 노출
        assertThat(result.projectMembers()).hasSize(1);
    }

    @Test
    @DisplayName("getByProjectId_권한이_없으면_PROJECT_ACCESS_DENIED")
    void 단건_권한없으면_거부() {
        // given
        Long projectId = 10L;
        Long chapterId = 3L;
        Long requesterMemberId = 8500L;
        given(loadProjectPort.getById(projectId))
            .willReturn(project(projectId, 9999L, chapterId));
        given(projectStatisticsAccessPolicy.canReadProjectStatistics(eq(requesterMemberId), any(Project.class)))
            .willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.getByProjectId(projectId, requesterMemberId))
            .isInstanceOf(ProjectDomainException.class);
    }

    /** 멤버 1명짜리 단일 프로젝트 지부 통계 데이터 stub (집계 경로 통과용). */
    private void givenSingleProjectChapter(Long chapterId, Long gisuId) {
        given(loadProjectStatisticsPort.listProjectsByChapterId(chapterId))
            .willReturn(List.of(projectRow(10L, gisuId, chapterId)));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId)).willReturn(List.of());
        given(loadProjectStatisticsPort.listActiveMembersByChapterId(chapterId))
            .willReturn(List.of(memberRow(10L, 101L, 1001L, ChallengerPart.WEB)));
        given(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(Set.of(10L))).willReturn(List.of());
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of(challenger(1001L, gisuId, ChallengerPart.WEB)));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1001L))).willReturn(Map.of(1001L, 501L));
    }

    /** 멤버 1명짜리 단건 프로젝트 통계 데이터 stub (집계 경로 통과용). */
    private void givenSingleProject(Long projectId, Long gisuId, Long chapterId) {
        given(loadProjectStatisticsPort.getProjectById(projectId))
            .willReturn(projectRow(projectId, gisuId, chapterId));
        given(loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId)).willReturn(List.of());
        given(loadProjectStatisticsPort.listActiveMembersByProjectId(projectId))
            .willReturn(List.of(memberRow(projectId, 101L, 1001L, ChallengerPart.WEB)));
        given(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(Set.of(projectId)))
            .willReturn(List.of());
        given(getChallengerUseCase.listByChapterId(chapterId))
            .willReturn(List.of(challenger(1001L, gisuId, ChallengerPart.WEB)));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1001L))).willReturn(Map.of(1001L, 501L));
    }

    private static Project project(Long id, Long ownerMemberId, Long chapterId) {
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Project project = constructor.newInstance();
            ReflectionTestUtils.setField(project, "id", id);
            ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerMemberId);
            ReflectionTestUtils.setField(project, "chapterId", chapterId);
            return project;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ProjectStatisticsProjectRow projectRow(Long projectId, Long gisuId, Long chapterId) {
        return new ProjectStatisticsProjectRow(projectId, gisuId, chapterId);
    }

    private static ProjectStatisticsMatchingRoundRow roundRow(
        Long matchingRoundId,
        MatchingType matchingRoundType,
        MatchingPhase matchingRoundPhase
    ) {
        return new ProjectStatisticsMatchingRoundRow(matchingRoundId, matchingRoundType, matchingRoundPhase);
    }

    private static ProjectStatisticsMemberRow memberRow(
        Long projectId, Long projectMemberId, Long memberId, ChallengerPart part
    ) {
        return new ProjectStatisticsMemberRow(projectId, projectMemberId, memberId, part, ProjectMemberStatus.ACTIVE);
    }

    private static ProjectStatisticsApplicationRow applicationRow(
        Long projectId,
        Long applicantMemberId,
        Long applicationId,
        ProjectApplicationStatus status,
        Long matchingRoundId,
        MatchingType matchingRoundType,
        MatchingPhase matchingRoundPhase
    ) {
        return new ProjectStatisticsApplicationRow(
            projectId,
            applicantMemberId,
            applicationId,
            status,
            matchingRoundId,
            matchingRoundType,
            matchingRoundPhase
        );
    }

    private static ProjectStatisticsApprovedApplicationRow approvedApplicationRow(
        Long projectId,
        Long applicantMemberId,
        Long applicationId,
        Long matchingRoundId,
        MatchingType matchingRoundType,
        MatchingPhase matchingRoundPhase,
        String matchingRoundStartsAt
    ) {
        return new ProjectStatisticsApprovedApplicationRow(
            projectId,
            applicantMemberId,
            applicationId,
            matchingRoundId,
            matchingRoundType,
            matchingRoundPhase,
            Instant.parse(matchingRoundStartsAt)
        );
    }

    private static ChallengerInfo challenger(Long memberId, Long gisuId, ChallengerPart part) {
        return challenger(memberId, gisuId, part, ChallengerStatus.ACTIVE);
    }

    private static ChallengerInfo challenger(
        Long memberId, Long gisuId, ChallengerPart part, ChallengerStatus status
    ) {
        return ChallengerInfo.builder()
            .memberId(memberId)
            .gisuId(gisuId)
            .part(part)
            .challengerStatus(status)
            .build();
    }
}
