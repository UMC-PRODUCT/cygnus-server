package com.umc.product.organization.application.service;

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

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductPart;
import com.umc.product.organization.domain.UmcProductSquad;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC Product 조직도 조회 서비스")
class UmcProductOrganizationChartQueryServiceTest {

    @Mock
    LoadUmcProductChapterPort loadUmcProductChapterPort;
    @Mock
    LoadUmcProductPartPort loadUmcProductPartPort;
    @Mock
    LoadUmcProductSquadPort loadUmcProductSquadPort;
    @Mock
    UmcProductDateProvider umcProductDateProvider;

    @InjectMocks
    UmcProductOrganizationChartQueryService sut;

    @Test
    void KST_오늘을_기준으로_활성_Chapter_Part_Squad를_조회한다() {
        LocalDate today = LocalDate.of(2026, 7, 13);
        UmcProductChapter chapter = UmcProductChapter.create(
            "DEVELOP", "개발", null, 1, true
        );
        ReflectionTestUtils.setField(chapter, "id", 1L);
        UmcProductPart part = UmcProductPart.create(
            chapter, "SERVER", "Server", null, 1, true
        );
        ReflectionTestUtils.setField(part, "id", 2L);
        UmcProductSquad squad = UmcProductSquad.create(
            "RECRUIT", "모집", null, today, null, 1, true
        );
        ReflectionTestUtils.setField(squad, "id", 3L);
        given(umcProductDateProvider.today()).willReturn(today);
        given(loadUmcProductChapterPort.listAll(true)).willReturn(List.of(chapter));
        given(loadUmcProductPartPort.listByChapterIds(List.of(1L), true)).willReturn(List.of(part));
        given(loadUmcProductSquadPort.listAll(true, today)).willReturn(List.of(squad));

        UmcProductOrganizationChartInfo result = sut.getCurrent();

        assertThat(result.chapters()).singleElement().satisfies(chapterInfo -> {
            assertThat(chapterInfo.chapter().chapterId()).isEqualTo(1L);
            assertThat(chapterInfo.parts()).singleElement()
                .satisfies(partInfo -> assertThat(partInfo.partId()).isEqualTo(2L));
        });
        assertThat(result.squads()).singleElement()
            .satisfies(squadInfo -> assertThat(squadInfo.squadId()).isEqualTo(3L));
        then(loadUmcProductSquadPort).should().listAll(true, today);
    }
}
