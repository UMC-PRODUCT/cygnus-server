package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.exception.ProjectDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 매칭 차수 조회 서비스")
class ProjectMatchingRoundQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-02T00:00:00Z");

    @Mock LoadProjectMatchingRoundPort loadPort;

    @Test
    @DisplayName("시간·지부 조합에 따라 open, chapter, 전체 차수를 조회한다")
    void 목록_조회_분기를_구분한다() {
        ProjectMatchingRound round = round(1L);
        given(loadPort.listOpenAt(10L, NOW)).willReturn(List.of(round));
        given(loadPort.listByChapterId(10L)).willReturn(List.of(round));
        given(loadPort.listAll()).willReturn(List.of(round));
        ProjectMatchingRoundQueryService sut = new ProjectMatchingRoundQueryService(loadPort);

        assertThat(sut.list(10L, NOW)).extracting(info -> info.id()).containsExactly(1L);
        assertThat(sut.list(10L, null)).extracting(info -> info.id()).containsExactly(1L);
        assertThat(sut.list(null, null)).extracting(info -> info.id()).containsExactly(1L);
        assertThatThrownBy(() -> sut.list(null, NOW)).isInstanceOf(ProjectDomainException.class);
    }

    @Test
    @DisplayName("batch 조회는 빈 입력을 단축하고 원본 조회 순서를 보존한다")
    void batch_조회는_빈_입력과_순서를_처리한다() {
        ProjectMatchingRound first = round(1L);
        ProjectMatchingRound second = round(2L);
        given(loadPort.listByIds(List.of(2L, 1L))).willReturn(List.of(second, first));
        ProjectMatchingRoundQueryService sut = new ProjectMatchingRoundQueryService(loadPort);

        assertThat(sut.findAllByIds(List.of())).isEmpty();
        assertThat(sut.findAllByIds(List.of(2L, 1L))).containsOnlyKeys(2L, 1L);
        assertThat(sut.findAllByIds(List.of(2L, 1L)).keySet()).containsExactly(2L, 1L);
    }

    private ProjectMatchingRound round(Long id) {
        ProjectMatchingRound value = ProjectMatchingRound.create(
            "1차", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 10L,
            NOW.minusSeconds(3_600), NOW, NOW.plusSeconds(3_600)
        );
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
