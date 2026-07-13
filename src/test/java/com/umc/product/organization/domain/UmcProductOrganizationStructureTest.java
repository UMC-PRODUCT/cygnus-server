package com.umc.product.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.exception.OrganizationErrorCode;

class UmcProductOrganizationStructureTest {

    @Test
    void 조직_역할은_PartLead와_Product_Leadership만_제공한다() {
        assertThat(UmcProductPartRole.values())
            .containsExactly(UmcProductPartRole.MEMBER, UmcProductPartRole.PART_LEAD);
        assertThat(UmcProductLeadershipRole.values())
            .containsExactly(
                UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
                UmcProductLeadershipRole.UMC_PRODUCT_LEAD
            );
    }

    @Test
    void Chapter와_그_하위_Part를_생성한다() {
        UmcProductChapter chapter = UmcProductChapter.create(" CLIENT ", " 클라이언트 ", " 앱 제품 ", 1, true);
        UmcProductPart part = UmcProductPart.create(chapter, " IOS ", " iOS ", " iOS 제품 ", 2, true);

        assertThat(chapter.getCode()).isEqualTo("CLIENT");
        assertThat(chapter.getName()).isEqualTo("클라이언트");
        assertThat(part.getChapter()).isSameAs(chapter);
        assertThat(part.getCode()).isEqualTo("IOS");
    }

    @Test
    void Part는_Chapter_없이_생성할_수_없다() {
        assertThatThrownBy(() -> UmcProductPart.create(null, "SERVER", "Server", null, 1, true))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_REQUIRED);
    }

    @Test
    void Chapter와_Part의_코드와_이름은_필수다() {
        UmcProductChapter chapter = UmcProductChapter.create("SERVER", "Server", null, 1, true);

        assertThatThrownBy(() -> UmcProductChapter.create(" ", "Server", null, 1, true))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_CODE_REQUIRED);
        assertThatThrownBy(() -> UmcProductPart.create(chapter, "SERVER", " ", null, 1, true))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_PART_NAME_REQUIRED);
    }

    @Test
    void Part_수정에서는_Chapter를_변경하지_않는다() {
        UmcProductChapter chapter = UmcProductChapter.create("CLIENT", "클라이언트", null, 1, true);
        UmcProductPart part = UmcProductPart.create(chapter, "IOS", "iOS", null, 1, true);

        part.update("APPLE", "Apple", "애플 플랫폼", 3, false);

        assertThat(part.getChapter()).isSameAs(chapter);
        assertThat(part.getCode()).isEqualTo("APPLE");
        assertThat(part.getName()).isEqualTo("Apple");
        assertThat(part.getDescription()).isEqualTo("애플 플랫폼");
        assertThat(part.getSortOrder()).isEqualTo(3);
        assertThat(part.isActive()).isFalse();
    }

    @Test
    void Chapter와_Part_수정에서_설명을_생략하면_기존_설명을_유지한다() {
        UmcProductChapter chapter = UmcProductChapter.create(
            "CLIENT", "클라이언트", "기존 Chapter 설명", 1, true
        );
        UmcProductPart part = UmcProductPart.create(
            chapter, "IOS", "iOS", "기존 Part 설명", 1, true
        );

        chapter.update(null, null, null, null, false);
        part.update(null, null, null, null, false);

        assertThat(chapter.getDescription()).isEqualTo("기존 Chapter 설명");
        assertThat(part.getDescription()).isEqualTo("기존 Part 설명");
    }
}
