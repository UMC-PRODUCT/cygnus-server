package com.umc.product.community.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.adapter.in.web.dto.request.CreateLightningRequest;
import com.umc.product.community.adapter.in.web.dto.request.CreatePostRequest;
import com.umc.product.community.adapter.in.web.dto.request.UpdateLightningRequest;
import com.umc.product.community.adapter.in.web.dto.request.UpdatePostRequest;
import com.umc.product.community.domain.enums.Category;

class CommunityRequestResidualTest {

    private static final Instant MEET_AT = Instant.parse("2026-07-02T00:00:00Z");

    @Test
    @DisplayName("일반 게시글 요청은 command로 변환하고 blank와 번개 category를 거부한다")
    void 일반_게시글_요청은_command로_변환하고_잘못된_값을_거부한다() {
        assertThat(new CreatePostRequest("제목", "내용", Category.FREE).toCommand(1L).authorChallengerId())
            .isEqualTo(1L);
        assertThatThrownBy(() -> new CreatePostRequest(" ", "내용", Category.FREE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreatePostRequest("제목", " ", Category.FREE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreatePostRequest("제목", "내용", Category.LIGHTNING))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("일반 게시글 수정 요청은 command로 변환하고 blank를 거부한다")
    void 일반_게시글_수정_요청은_command로_변환하고_blank를_거부한다() {
        assertThat(new UpdatePostRequest("제목", "내용", Category.FREE).toCommand(10L).postId())
            .isEqualTo(10L);
        assertThatThrownBy(() -> new UpdatePostRequest(" ", "내용", Category.FREE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdatePostRequest("제목", " ", Category.FREE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("번개 생성 요청은 모든 필드를 command로 변환한다")
    void 번개_생성_요청은_모든_필드를_command로_변환한다() {
        var command = createLightning().toCommand(1L);

        assertThat(command.authorChallengerId()).isEqualTo(1L);
        assertThat(command.meetAt()).isEqualTo(MEET_AT);
        assertThat(command.location()).isEqualTo("강남");
    }

    @Test
    @DisplayName("번개 생성 요청은 blank·URL 형식·인원 경계를 거부한다")
    void 번개_생성_요청은_blank_URL_형식_인원_경계를_거부한다() {
        assertThatThrownBy(() -> new CreateLightningRequest(" ", "내용", MEET_AT, "강남", 2, "https://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateLightningRequest("제목", " ", MEET_AT, "강남", 2, "https://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateLightningRequest("제목", "내용", MEET_AT, " ", 2, "https://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateLightningRequest("제목", "내용", MEET_AT, "강남", 2, " "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateLightningRequest("제목", "내용", MEET_AT, "강남", 2, "ftp://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateLightningRequest("제목", "내용", MEET_AT, "강남", 0, "http://chat"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("번개 수정 요청은 모든 필드를 command로 변환한다")
    void 번개_수정_요청은_모든_필드를_command로_변환한다() {
        var command = updateLightning().toCommand(10L);

        assertThat(command.postId()).isEqualTo(10L);
        assertThat(command.maxParticipants()).isEqualTo(2);
        assertThat(command.openChatUrl()).isEqualTo("https://chat");
    }

    @Test
    @DisplayName("번개 수정 요청은 blank·URL 형식·인원 경계를 거부한다")
    void 번개_수정_요청은_blank_URL_형식_인원_경계를_거부한다() {
        assertThatThrownBy(() -> new UpdateLightningRequest(" ", "내용", MEET_AT, "강남", 2, "https://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateLightningRequest("제목", " ", MEET_AT, "강남", 2, "https://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateLightningRequest("제목", "내용", MEET_AT, " ", 2, "https://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateLightningRequest("제목", "내용", MEET_AT, "강남", 2, " "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateLightningRequest("제목", "내용", MEET_AT, "강남", 2, "ftp://chat"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateLightningRequest("제목", "내용", MEET_AT, "강남", 0, "http://chat"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private CreateLightningRequest createLightning() {
        return new CreateLightningRequest("제목", "내용", MEET_AT, "강남", 2, "https://chat");
    }

    private UpdateLightningRequest updateLightning() {
        return new UpdateLightningRequest("제목", "내용", MEET_AT, "강남", 2, "https://chat");
    }
}
