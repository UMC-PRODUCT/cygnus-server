package com.umc.product.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@DisplayName("챌린저 워크북")
class ChallengerWorkbookTest {

    @Test
    @DisplayName("원본 워크북 없이 생성할 수 없다")
    void createFailsWhenOriginalWorkbookIsNull() {
        assertThatThrownBy(() -> ChallengerWorkbook.create(null, 1L, 1L))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_NOT_FOUND);
    }
}
