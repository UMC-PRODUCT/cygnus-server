package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort.ChallengerWorkbookLookupKey;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SearchWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;

@ExtendWith(MockitoExtension.class)
class WeeklyBestWorkbookQueryServiceTest {

    @Mock private SearchWeeklyBestWorkbookPort searchWeeklyBestWorkbookPort;
    @Mock private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock private LoadMissionFeedbackPort loadMissionFeedbackPort;
    @Mock private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock private GetMemberUseCase getMemberUseCase;
    @InjectMocks private WeeklyBestWorkbookQueryService service;

    @Test
    @DisplayName("베스트 워크북 상세 조회는 회원·주차·스터디 그룹의 정확한 조합을 사용한다")
    void query_usesExactTripleKey() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"), 1L, false, "1주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", 20L);
        WeeklyBestWorkbook best = WeeklyBestWorkbook.create(weekly, 30L, 10L, "사유", 50L);
        ReflectionTestUtils.setField(best, "id", 60L);
        given(searchWeeklyBestWorkbookPort.searchBestWorkbooks(
            GetBestWorkbooksQuery.of(9L, null, null, null, null, null, 20).withMemberIds(null)
        )).willReturn(new PageImpl<>(List.of(best), PageRequest.of(0, 20), 1));
        given(loadChallengerWorkbookPort.listByLookupKeys(anyList())).willReturn(List.of());

        var page = service.searchBestWorkbooks(
            GetBestWorkbooksQuery.of(9L, null, null, null, null, null, 20)
        );

        ArgumentCaptor<List<ChallengerWorkbookLookupKey>> captor = ArgumentCaptor.forClass(List.class);
        verify(loadChallengerWorkbookPort).listByLookupKeys(captor.capture());
        assertThat(captor.getValue()).containsExactly(new ChallengerWorkbookLookupKey(30L, 20L, 10L));
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).challengerWorkbooks()).isEmpty();
    }
}
