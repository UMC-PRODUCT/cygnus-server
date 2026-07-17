package com.umc.product.storage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileUsageCoordinateTest {

    @Test
    @DisplayName("유효한 파일 사용 좌표를 생성한다")
    void 유효한_파일_사용_좌표를_생성한다() {
        // given
        String namespace = "organization.umc-product-member";
        String resourceKey = "member:10.profile_1";
        String slot = "profile-image";

        // when
        FileUsageCoordinate coordinate = FileUsageCoordinate.of(namespace, resourceKey, slot);

        // then
        assertThat(coordinate.usageNamespace()).isEqualTo(namespace);
        assertThat(coordinate.resourceKey()).isEqualTo(resourceKey);
        assertThat(coordinate.slot()).isEqualTo(slot);
    }

    @Test
    @DisplayName("namespace는 소문자 ASCII dot segment 문법을 따라야 한다")
    void namespace는_소문자_ASCII_dot_segment_문법을_따라야_한다() {
        assertThatThrownBy(() -> FileUsageCoordinate.of("Organization.member", "10", "profile-image"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("organization..member", "10", "profile-image"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("organization_member", "10", "profile-image"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("namespace는 100자를 초과할 수 없다")
    void namespace는_100자를_초과할_수_없다() {
        // given
        String validHundredCharacters = "a".repeat(32) + "." + "b".repeat(32) + "." + "c".repeat(32) + ".d";
        String tooLong = "a".repeat(32) + "." + "b".repeat(32) + "." + "c".repeat(32) + ".de";

        // when & then
        assertThat(FileUsageCoordinate.of(validHundredCharacters, "10", "default").usageNamespace())
            .hasSize(100);
        assertThatThrownBy(() -> FileUsageCoordinate.of(tooLong, "10", "default"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resource key는 canonical scalar 문법을 따라야 한다")
    void resource_key는_canonical_scalar_문법을_따라야_한다() {
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "-10", "profile-image"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "member 10", "profile-image"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "a".repeat(129), "profile-image"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("slot은 소문자 kebab 문법을 따라야 한다")
    void slot은_소문자_kebab_문법을_따라야_한다() {
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "10", "Profile-Image"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "10", "profile_image"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "10", "a".repeat(51)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("파일 사용 좌표의 모든 값은 필수다")
    void 파일_사용_좌표의_모든_값은_필수다() {
        assertThatThrownBy(() -> FileUsageCoordinate.of(null, "10", "default"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", null, "default"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileUsageCoordinate.of("member", "10", null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
