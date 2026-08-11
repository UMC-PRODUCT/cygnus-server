package com.umc.product.inhouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.exception.InhouseErrorCode;

class UmcProductOrganizationStructureTest {

    @Test
    void 조직_역할은_Product_Leadership만_제공한다() {
        assertThat(UmcProductLeadershipRole.values())
            .containsExactly(
                UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
                UmcProductLeadershipRole.UMC_PRODUCT_LEAD
            );
    }

    @Test
    void Chapter를_생성한다() {
        UmcProductChapter chapter = UmcProductChapter.create(" CLIENT ", " 클라이언트 ", " 앱 제품 ", 1, true);

        assertThat(chapter.getCode()).isEqualTo("CLIENT");
        assertThat(chapter.getName()).isEqualTo("클라이언트");
        assertThat(chapter.getDescription()).isEqualTo("앱 제품");
    }

    @Test
    void Chapter의_코드와_이름은_필수다() {
        assertThatThrownBy(() -> UmcProductChapter.create(" ", "Server", null, 1, true))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_CHAPTER_CODE_REQUIRED);
        assertThatThrownBy(() -> UmcProductChapter.create("SERVER", " ", null, 1, true))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_CHAPTER_NAME_REQUIRED);
    }

    @Test
    void Chapter를_수정한다() {
        UmcProductChapter chapter = UmcProductChapter.create("CLIENT", "클라이언트", null, 1, true);

        chapter.update("PRODUCT", "프로덕트", "제품 조직", 3, false);

        assertThat(chapter.getCode()).isEqualTo("PRODUCT");
        assertThat(chapter.getName()).isEqualTo("프로덕트");
        assertThat(chapter.getDescription()).isEqualTo("제품 조직");
        assertThat(chapter.getSortOrder()).isEqualTo(3);
        assertThat(chapter.isActive()).isFalse();
    }

    @Test
    void Chapter_수정에서_설명을_생략하면_기존_설명을_유지한다() {
        UmcProductChapter chapter = UmcProductChapter.create(
            "CLIENT", "클라이언트", "기존 Chapter 설명", 1, true
        );

        chapter.update(null, null, null, null, false);

        assertThat(chapter.getDescription()).isEqualTo("기존 Chapter 설명");
    }
}
