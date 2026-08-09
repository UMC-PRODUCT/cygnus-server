package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

@ExtendWith(MockitoExtension.class)
class OriginalWorkbookQueryServiceTest {

    @Mock
    private LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock
    private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock
    private GetStudyGroupUseCase getStudyGroupUseCase;
    @InjectMocks
    private OriginalWorkbookQueryService service;

    private OriginalWorkbook workbook;

    @BeforeEach
    void setUp() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"), 1L, false, "1주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        workbook = OriginalWorkbook.createAsDraft(weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN);
        ReflectionTestUtils.setField(workbook, "id", 1L);
    }

    @Test
    @DisplayName("요청자가 같은 기수와 파트의 스터디 그룹 구성원이면 원본 워크북을 조회한다")
    void matchedStudyGroup_allowed() {
        given(loadOriginalWorkbookPort.getById(1L)).willReturn(workbook);
        given(getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(2L, 9L, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(StudyGroupInfo.create(
                10L, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(), List.of(2L)
            )));
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(1L)).willReturn(List.of());

        assertThat(service.getById(1L, 2L).originalWorkbookId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 기수와 파트의 스터디 그룹에 속하지 않으면 원본 워크북 조회를 거부한다")
    void unmatchedStudyGroup_denied() {
        given(loadOriginalWorkbookPort.getById(1L)).willReturn(workbook);
        given(getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(2L, 9L, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L, 2L))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
    }
}
