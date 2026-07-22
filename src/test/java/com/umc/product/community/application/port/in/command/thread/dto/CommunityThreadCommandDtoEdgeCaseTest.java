package com.umc.product.community.application.port.in.command.thread.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@DisplayName("Community thread command DTO 경계")
class CommunityThreadCommandDtoEdgeCaseTest {

    @Test
    @DisplayName("create command는 텍스트를 정규화하고 null invitee를 빈 목록으로 고정한다")
    void normalizesValidCreateCommand() {
        CreateCommunityThreadCommand command = new CreateCommunityThreadCommand(
            1L,
            "  제목  ",
            null,
            CommunityThreadCategory.FREE,
            "  💬  ",
            null
        );

        assertThat(command.title()).isEqualTo("제목");
        assertThat(command.description()).isNull();
        assertThat(command.icon()).isEqualTo("💬");
        assertThat(command.inviteeMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("create command는 필수값·길이·중복·자기 초대 위반을 동일 domain code로 거부한다")
    void rejectsInvalidCreateCommands() {
        assertInvalid(() -> create(null, "제목", null, CommunityThreadCategory.FREE, "💬", List.of()));
        assertInvalid(() -> create(1L, " ", null, CommunityThreadCategory.FREE, "💬", List.of()));
        assertInvalid(() -> create(1L, "가".repeat(81), null, CommunityThreadCategory.FREE, "💬", List.of()));
        assertInvalid(() -> create(1L, "제목", "가".repeat(501), CommunityThreadCategory.FREE, "💬", List.of()));
        assertInvalid(() -> create(1L, "제목", null, null, "💬", List.of()));
        assertInvalid(() -> create(1L, "제목", null, CommunityThreadCategory.FREE, " ", List.of()));
        assertInvalid(() -> create(1L, "제목", null, CommunityThreadCategory.FREE, "가".repeat(33), List.of()));
        assertInvalid(() -> create(1L, "제목", null, CommunityThreadCategory.FREE, "💬", List.of(2L, 2L)));
        assertInvalid(() -> create(1L, "제목", null, CommunityThreadCategory.FREE, "💬",
            Arrays.asList(2L, null)));
        assertInvalid(() -> create(1L, "제목", null, CommunityThreadCategory.FREE, "💬", List.of(1L)));
    }

    @Test
    @DisplayName("kick command는 양수 ID와 actor·target 분리를 강제한다")
    void validatesKickCommand() {
        assertThat(new KickCommunityThreadMemberCommand(1L, 2L, 3L).memberId()).isEqualTo(3L);
        assertInvalid(() -> new KickCommunityThreadMemberCommand(0L, 2L, 3L));
        assertInvalid(() -> new KickCommunityThreadMemberCommand(1L, 2L, 2L));
    }

    @Test
    @DisplayName("공통 validation은 empty 허용 여부와 null required 값을 fail-fast로 구분한다")
    void validatesSharedCommandConstraints() {
        assertThat(CommunityThreadCommandValidation.optionalText(null, 10)).isNull();
        assertThat(CommunityThreadCommandValidation.uniqueIds(null, true)).isEmpty();
        assertInvalid(() -> CommunityThreadCommandValidation.uniqueIds(null, false));
        assertInvalid(() -> CommunityThreadCommandValidation.required(null));
    }

    private CreateCommunityThreadCommand create(
        Long actorMemberId,
        String title,
        String description,
        CommunityThreadCategory category,
        String icon,
        List<Long> invitees
    ) {
        return new CreateCommunityThreadCommand(
            actorMemberId,
            title,
            description,
            category,
            icon,
            invitees
        );
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_INVALID_COMMAND);
    }
}
