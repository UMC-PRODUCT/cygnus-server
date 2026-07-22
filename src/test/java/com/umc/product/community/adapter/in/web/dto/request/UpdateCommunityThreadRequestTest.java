package com.umc.product.community.adapter.in.web.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.application.port.in.command.thread.dto.UpdateCommunityThreadCommand;
import com.umc.product.community.domain.enums.CommunityThreadCategory;

@DisplayName("UpdateCommunityThreadRequest")
class UpdateCommunityThreadRequestTest {

    @Test
    @DisplayName("명시된 patch 필드를 조회하고 command로 그대로 변환한다")
    void exposesPresentFieldsAndConvertsToCommand() {
        UpdateCommunityThreadRequest request = new UpdateCommunityThreadRequest();
        request.setTitle("새 제목");
        request.setDescription("새 설명");
        request.setCategory(CommunityThreadCategory.PROJECT);
        request.setIcon("💬");

        assertThat(request.getTitle()).isEqualTo("새 제목");
        assertThat(request.getDescription()).isEqualTo("새 설명");
        assertThat(request.getCategory()).isEqualTo(CommunityThreadCategory.PROJECT);
        assertThat(request.getIcon()).isEqualTo("💬");
        assertThat(request.isValidPatch()).isTrue();

        UpdateCommunityThreadCommand command = request.toCommand(1L, 2L);
        assertThat(command.title()).isEqualTo("새 제목");
        assertThat(command.description()).isEqualTo("새 설명");
        assertThat(command.category()).isEqualTo(CommunityThreadCategory.PROJECT);
        assertThat(command.icon()).isEqualTo("💬");
    }

    @Test
    @DisplayName("blank icon과 단일 grapheme이 아닌 icon은 patch validation에서 거절한다")
    void rejectsInvalidIconBoundaries() {
        UpdateCommunityThreadRequest blank = new UpdateCommunityThreadRequest();
        blank.setIcon("   ");
        UpdateCommunityThreadRequest multipleGraphemes = new UpdateCommunityThreadRequest();
        multipleGraphemes.setIcon("AB");

        assertThat(blank.isValidPatch()).isFalse();
        assertThat(multipleGraphemes.isValidPatch()).isFalse();
    }
}
