package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWorkbookSubmissionPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.global.exception.NotImplementedException;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@DisplayName("Curriculum 기타 query service")
class CurriculumOtherQueryServiceTest {

    @Test
    @DisplayName("주차 query는 조회한 entity를 info로 변환한다")
    void gets_weekly_curriculum_info() {
        LoadWeeklyCurriculumPort port = mock(LoadWeeklyCurriculumPort.class);
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        ReflectionTestUtils.setField(curriculum, "id", 1L);
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            curriculum, 2L, true, "부록",
            Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-07T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", 10L);
        given(port.getById(10L)).willReturn(weekly);

        var result = new WeeklyCurriculumQueryService(port).getWeeklyCurriculum(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.curriculumId()).isEqualTo(1L);
        assertThat(result.isExtra()).isTrue();
        assertThat(result.weekNo()).isEqualTo(2L);
    }

    @Test
    @DisplayName("구현 전 query는 silent null 대신 명시적인 예외를 반환한다")
    void not_implemented_queries_are_explicit() {
        var original = new OriginalWorkbookQueryService(mock(LoadOriginalWorkbookPort.class));
        var challenger = new ChallengerWorkbookQueryService(
            mock(LoadWorkbookSubmissionPort.class), mock(LoadChallengerWorkbookPort.class),
            mock(GetStudyGroupUseCase.class), mock(GetFileUseCase.class)
        );
        var best = new WeeklyBestWorkbookQueryService();

        assertThatThrownBy(() -> original.getById(1L)).isInstanceOf(NotImplementedException.class);
        assertThatThrownBy(() -> challenger.getById(1L)).isInstanceOf(NotImplementedException.class);
        assertThatThrownBy(() -> best.searchBestWorkbooks(
            new GetBestWorkbooksQuery(null, null, null, null, null, null, 0)))
            .isInstanceOf(NotImplementedException.class);
    }
}
