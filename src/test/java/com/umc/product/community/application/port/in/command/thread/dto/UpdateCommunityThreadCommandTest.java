package com.umc.product.community.application.port.in.command.thread.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.exception.CommunityDomainException;

@DisplayName("UpdateCommunityThreadCommand")
class UpdateCommunityThreadCommandTest {

    @Test
    @DisplayName("description omitted는 미제공으로 유지한다")
    void omittedDescriptionIsNotProvided() {
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            1L,
            2L,
            "제목",
            null,
            null,
            null
        );

        assertThat(command.descriptionProvided()).isFalse();
        assertThat(command.description()).isNull();
    }

    @Test
    @DisplayName("description blank는 제공된 null clear 값으로 정규화한다")
    void blankDescriptionIsProvidedAsClearValue() {
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            1L,
            2L,
            null,
            "  \t",
            true,
            CommunityThreadCategory.STUDY,
            null
        );

        assertThat(command.descriptionProvided()).isTrue();
        assertThat(command.description()).isNull();
    }

    @Test
    @DisplayName("description 제공 여부가 patch field 존재성 검증에 반영된다")
    void descriptionPresenceCountsAsPatchField() {
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            1L,
            2L,
            null,
            null,
            true,
            null,
            null
        );

        assertThat(command.descriptionProvided()).isTrue();
        assertThat(command.description()).isNull();
    }

    @Test
    @DisplayName("어떤 필드도 제공하지 않으면 invalid command다")
    void omittedFieldsAreRejected() {
        assertThatThrownBy(() -> new UpdateCommunityThreadCommand(
            1L,
            2L,
            null,
            null,
            false,
            null,
            null
        ))
            .isInstanceOf(CommunityDomainException.class);
    }
}
