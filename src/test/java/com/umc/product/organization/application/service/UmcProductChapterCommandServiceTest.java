package com.umc.product.organization.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT Chapter 명령 서비스")
class UmcProductChapterCommandServiceTest {

    @Mock
    LoadUmcProductChapterPort loadUmcProductChapterPort;
    @Mock
    SaveUmcProductChapterPort saveUmcProductChapterPort;
    @Mock
    LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    @Mock
    UmcProductAccessPolicy umcProductAccessPolicy;

    @InjectMocks
    UmcProductChapterCommandService chapterService;

    @Test
    void 소속_이력이_있는_Chapter는_삭제할_수_없다() {
        UmcProductChapter chapter = chapter(1L);
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductChapterPort.getByIdWithLock(1L)).willReturn(chapter);
        given(loadUmcProductChapterMembershipPort.existsByChapterId(1L)).willReturn(true);

        assertThatThrownBy(() -> chapterService.delete(1L, 100L))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_HAS_MEMBERSHIPS);

        then(saveUmcProductChapterPort).should(never()).delete(any());
        then(loadUmcProductChapterPort).should(never()).getById(1L);
    }

    @Test
    void Chapter_수정은_비관적_잠금으로_조회한다() {
        UmcProductChapter chapter = chapter(1L);
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(true);
        given(loadUmcProductChapterPort.getByIdWithLock(1L)).willReturn(chapter);

        chapterService.update(UpdateUmcProductChapterCommand.of(
            1L, 100L, null, "수정된 개발", null, null, false
        ));

        then(loadUmcProductChapterPort).should().getByIdWithLock(1L);
        then(loadUmcProductChapterPort).should(never()).getById(1L);
        then(saveUmcProductChapterPort).should().save(chapter);
    }

    private UmcProductChapter chapter(Long id) {
        UmcProductChapter chapter = UmcProductChapter.create(
            "DEVELOP", "개발", "개발 Chapter", 1, true
        );
        ReflectionTestUtils.setField(chapter, "id", id);
        return chapter;
    }

}
