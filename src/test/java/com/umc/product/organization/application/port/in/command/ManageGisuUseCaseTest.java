package com.umc.product.organization.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.application.port.in.command.dto.CreateGisuCommand;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.support.UseCaseTestSupport;
import com.umc.product.support.fixture.GisuFixture;

class ManageGisuUseCaseTest extends UseCaseTestSupport {

    @Autowired
    private ManageGisuUseCase manageGisuUseCase;

    @Autowired
    private GetGisuUseCase getGisuUseCase;

    @Autowired
    private GisuFixture gisuFixture;

    @Test
    void 신규_기수를_트랙방식으로_생성한다() {
        // given
        CreateGisuCommand command = new CreateGisuCommand(
            10L,
            Instant.parse("2025-03-01T00:00:00Z"),
            Instant.parse("2025-08-31T23:59:59Z")
        );

        // when
        Long gisuId = manageGisuUseCase.create(command);

        // then
        assertThat(gisuId).isNotNull();
        GisuInfo savedGisu = getGisuUseCase.getById(gisuId);
        assertThat(savedGisu.generation()).isEqualTo(10L);
        assertThat(savedGisu.isActive()).isFalse();
        assertThat(savedGisu.learningType()).isEqualTo(GisuLearningType.TRACK);
    }

    @Test
    void 이미_존재하는_기수_번호로_생성하면_예외가_발생한다() {
        // given
        gisuFixture.비활성_기수(10L);

        CreateGisuCommand command = new CreateGisuCommand(
            10L,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-08-31T23:59:59Z")
        );

        // when & then
        assertThatThrownBy(() -> manageGisuUseCase.create(command))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.GISU_ALREADY_EXISTS);
    }

    @Test
    void 기수를_삭제한다() {
        // given
        Long gisuId = gisuFixture.비활성_기수(11L).getId();

        // when
        manageGisuUseCase.deleteGisu(gisuId);

        // then
        assertThatThrownBy(() -> getGisuUseCase.getById(gisuId))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.GISU_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_기수를_삭제하면_예외가_발생한다() {
        // given
        Long nonExistentGisuId = 9999L;

        // when & then
        assertThatThrownBy(() -> manageGisuUseCase.deleteGisu(nonExistentGisuId))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.GISU_NOT_FOUND);
    }

    @Test
    void 활성_기수를_변경한다() {
        // given
        Long oldActiveGisuId = gisuFixture.비활성_기수(8L).getId();
        manageGisuUseCase.updateActiveGisu(oldActiveGisuId);
        GisuInfo oldActiveGisu = getGisuUseCase.getById(oldActiveGisuId);
        Long newGisuId = gisuFixture.비활성_기수(9L).getId();

        // when
        manageGisuUseCase.updateActiveGisu(newGisuId);

        // then
        GisuInfo oldGisuInfo = getGisuUseCase.getById(oldActiveGisu.gisuId());
        GisuInfo newGisuInfo = getGisuUseCase.getById(newGisuId);

        assertThat(oldGisuInfo.isActive()).isFalse();
        assertThat(newGisuInfo.isActive()).isTrue();
    }
}
