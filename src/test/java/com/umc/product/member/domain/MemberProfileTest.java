package com.umc.product.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberProfile 도메인")
class MemberProfileTest {

    @Test
    @DisplayName("fromLinks는 링크 타입별 컬럼에 값을 매핑한다")
    void fromLinks는_링크_타입별_컬럼에_값을_매핑한다() {
        MemberProfile profile = MemberProfile.fromLinks(List.of(
            new LinkTypeAndLink(MemberProfileLinkType.LINKEDIN, "https://linkedin.com/in/umc"),
            new LinkTypeAndLink(MemberProfileLinkType.INSTAGRAM, "https://instagram.com/umc"),
            new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "https://github.com/umc"),
            new LinkTypeAndLink(MemberProfileLinkType.BLOG, "https://blog.example.com"),
            new LinkTypeAndLink(MemberProfileLinkType.PERSONAL, "https://umc.example.com")
        ));

        assertThat(profile.getLinkedIn()).isEqualTo("https://linkedin.com/in/umc");
        assertThat(profile.getInstagram()).isEqualTo("https://instagram.com/umc");
        assertThat(profile.getGithub()).isEqualTo("https://github.com/umc");
        assertThat(profile.getBlog()).isEqualTo("https://blog.example.com");
        assertThat(profile.getPersonal()).isEqualTo("https://umc.example.com");
    }

    @Test
    @DisplayName("updateLinks는 기존 링크를 모두 초기화한 뒤 제공된 링크만 반영한다")
    void updateLinks는_기존_링크를_모두_초기화한_뒤_제공된_링크만_반영한다() {
        MemberProfile profile = MemberProfile.fromLinks(List.of(
            new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "https://github.com/old"),
            new LinkTypeAndLink(MemberProfileLinkType.BLOG, "https://old.example.com")
        ));

        profile.updateLinks(List.of(
            new LinkTypeAndLink(MemberProfileLinkType.LINKEDIN, "https://linkedin.com/new"),
            new LinkTypeAndLink(MemberProfileLinkType.INSTAGRAM, "https://instagram.com/new"),
            new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "https://github.com/new"),
            new LinkTypeAndLink(MemberProfileLinkType.BLOG, "https://blog.example.com/new"),
            new LinkTypeAndLink(MemberProfileLinkType.PERSONAL, "https://new.example.com")
        ));

        assertThat(profile.getLinkedIn()).isEqualTo("https://linkedin.com/new");
        assertThat(profile.getInstagram()).isEqualTo("https://instagram.com/new");
        assertThat(profile.getGithub()).isEqualTo("https://github.com/new");
        assertThat(profile.getBlog()).isEqualTo("https://blog.example.com/new");
        assertThat(profile.getPersonal()).isEqualTo("https://new.example.com");
    }
}
