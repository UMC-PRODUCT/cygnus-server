package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductOrganizationChartInfo;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.inhouse.domain.UmcProductChapter;
import com.umc.product.inhouse.domain.UmcProductSquad;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 조직도 조회 서비스")
class UmcProductOrganizationChartQueryServiceTest {

    @Mock
    LoadUmcProductChapterPort loadUmcProductChapterPort;
    @Mock
    LoadUmcProductSquadPort loadUmcProductSquadPort;
    @Mock
    UmcProductDateProvider umcProductDateProvider;

    @InjectMocks
    UmcProductOrganizationChartQueryService sut;

    @Test
    void KST_오늘을_기준으로_활성_Chapter와_Squad를_조회한다() {
        LocalDate today = LocalDate.of(2026, 7, 13);
        UmcProductChapter chapter = UmcProductChapter.create(
            "DEVELOP", "개발", null, 1, true
        );
        ReflectionTestUtils.setField(chapter, "id", 1L);
        UmcProductSquad squad = UmcProductSquad.create(
            "RECRUIT", "모집", null, today, null, 1, true
        );
        ReflectionTestUtils.setField(squad, "id", 3L);
        given(umcProductDateProvider.today()).willReturn(today);
        given(loadUmcProductChapterPort.listAll(true)).willReturn(List.of(chapter));
        given(loadUmcProductSquadPort.listAll(true, today)).willReturn(List.of(squad));

        UmcProductOrganizationChartInfo result = sut.getCurrent();

        assertThat(result.chapters()).singleElement()
            .satisfies(chapterInfo -> assertThat(chapterInfo.chapterId()).isEqualTo(1L));
        assertThat(result.squads()).singleElement()
            .satisfies(squadInfo -> assertThat(squadInfo.squadId()).isEqualTo(3L));
        then(loadUmcProductSquadPort).should().listAll(true, today);
    }
}
