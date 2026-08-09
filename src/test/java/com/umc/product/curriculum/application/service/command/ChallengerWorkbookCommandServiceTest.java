package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

@ExtendWith(MockitoExtension.class)
class ChallengerWorkbookCommandServiceTest {

    @Mock private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock private LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock private SaveChallengerWorkbookPort saveChallengerWorkbookPort;
    @Mock private GetChallengerUseCase getChallengerUseCase;
    @Mock private GetStudyGroupUseCase getStudyGroupUseCase;
    @InjectMocks private ChallengerWorkbookCommandService service;

    private OriginalWorkbook original;

    @BeforeEach
    void setUp() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"), 1L, false, "1주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        original = OriginalWorkbook.createAsReady(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        original.changeStatus(OriginalWorkbookStatus.RELEASED, 1L);
        ReflectionTestUtils.setField(original, "id", 100L);
    }

    @Test
    @DisplayName("self deploy는 기수와 파트에 대응하는 실제 스터디 그룹 ID를 저장한다")
    void deploy_persistsResolvedStudyGroup() {
        given(loadOriginalWorkbookPort.batchGetByIds(List.of(100L))).willReturn(List.of(original));
        given(getChallengerUseCase.getAllByMemberId(30L)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(30L).gisuId(9L).part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE).build()
        ));
        given(getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(30L, 9L, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(StudyGroupInfo.create(
                10L, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(50L), List.of(30L)
            )));
        given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(30L, List.of(100L)))
            .willReturn(List.of());
        given(saveChallengerWorkbookPort.save(any(ChallengerWorkbook.class))).willAnswer(invocation -> {
            ChallengerWorkbook workbook = invocation.getArgument(0);
            ReflectionTestUtils.setField(workbook, "id", 200L);
            return workbook;
        });

        var result = service.batchDeploy(DeployChallengerWorkbookCommand.builder()
            .requestedMemberId(30L).originalWorkbookIds(List.of(100L)).build());

        assertThat(result).singleElement().extracting("receivedStudyGroupId").isEqualTo(10L);
    }

    @Test
    @DisplayName("대응하는 스터디 그룹이 없으면 self deploy를 거부한다")
    void deployWithoutStudyGroupRejected() {
        given(loadOriginalWorkbookPort.batchGetByIds(List.of(100L))).willReturn(List.of(original));
        given(getChallengerUseCase.getAllByMemberId(30L)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(30L).gisuId(9L).part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE).build()
        ));
        given(getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(30L, 9L, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.batchDeploy(DeployChallengerWorkbookCommand.builder()
            .requestedMemberId(30L).originalWorkbookIds(List.of(100L)).build()))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
        verify(saveChallengerWorkbookPort, never()).save(any());
    }

    @Test
    @DisplayName("일괄 배포는 같은 기수와 파트의 스터디 그룹 조회 결과를 재사용한다")
    void batchDeployCachesStudyGroupByGisuAndPart() {
        OriginalWorkbook second = OriginalWorkbook.createAsReady(
            original.getWeeklyCurriculum(), "두 번째 워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        second.changeStatus(OriginalWorkbookStatus.RELEASED, 1L);
        ReflectionTestUtils.setField(second, "id", 101L);
        given(loadOriginalWorkbookPort.batchGetByIds(List.of(100L, 101L)))
            .willReturn(List.of(original, second));
        given(getChallengerUseCase.getAllByMemberId(30L)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(30L).gisuId(9L).part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE).build()
        ));
        given(getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(30L, 9L, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(StudyGroupInfo.create(
                10L, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(50L), List.of(30L)
            )));
        given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
            30L, List.of(100L, 101L)
        )).willReturn(List.of());
        given(saveChallengerWorkbookPort.save(any(ChallengerWorkbook.class))).willAnswer(invocation -> {
            ChallengerWorkbook workbook = invocation.getArgument(0);
            ReflectionTestUtils.setField(workbook, "id", 200L + workbook.getOriginalWorkbook().getId());
            return workbook;
        });

        var result = service.batchDeploy(DeployChallengerWorkbookCommand.builder()
            .requestedMemberId(30L).originalWorkbookIds(List.of(100L, 101L)).build());

        assertThat(result).hasSize(2);
        verify(getStudyGroupUseCase).findByMemberIdAndGisuIdAndPart(
            30L, 9L, ChallengerPart.SPRINGBOOT
        );
    }

    @Test
    @DisplayName("기존 워크북의 그룹과 현재 소속 그룹이 다르면 self deploy를 거부한다")
    void deployWithStaleStudyGroupRejected() {
        ChallengerWorkbook existing = ChallengerWorkbook.create(original, 30L, 99L);
        ReflectionTestUtils.setField(existing, "id", 200L);
        given(loadOriginalWorkbookPort.batchGetByIds(List.of(100L))).willReturn(List.of(original));
        given(getChallengerUseCase.getAllByMemberId(30L)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(30L).gisuId(9L).part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE).build()
        ));
        given(getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(30L, 9L, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(StudyGroupInfo.create(
                10L, "현재 그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH,
                List.of(50L), List.of(30L)
            )));
        given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(30L, List.of(100L)))
            .willReturn(List.of(existing));

        assertThatThrownBy(() -> service.batchDeploy(DeployChallengerWorkbookCommand.builder()
            .requestedMemberId(30L).originalWorkbookIds(List.of(100L)).build()))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
        verify(saveChallengerWorkbookPort, never()).save(any());
    }

    @Test
    @DisplayName("연결된 제출물이 있는 챌린저 워크북은 삭제하지 않는다")
    void delete_withSubmission_rejected() {
        ChallengerWorkbook workbook = ChallengerWorkbook.create(original, 30L, 10L);
        ReflectionTestUtils.setField(workbook, "id", 200L);
        given(loadChallengerWorkbookPort.getById(200L)).willReturn(workbook);
        given(loadMissionSubmissionPort.existsByChallengerWorkbookId(200L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(DeleteChallengerWorkbookCommand.builder()
            .challengerWorkbookId(200L).requestedMemberId(50L).build()))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS);
        verify(saveChallengerWorkbookPort, never()).delete(any());
    }
}
