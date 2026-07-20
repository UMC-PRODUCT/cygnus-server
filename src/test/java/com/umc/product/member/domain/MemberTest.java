package com.umc.product.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

@DisplayName("Member 도메인")
class MemberTest {

    @Test
    @DisplayName("create는 ACTIVE 상태 회원을 생성한다")
    void create는_ACTIVE_상태_회원을_생성한다() {
        Member member = Member.create("홍길동", "길동", "gildong@example.com", 1L, "profile-file-id");

        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getNickname()).isEqualTo("길동");
        assertThat(member.getEmail()).isEqualTo("gildong@example.com");
        assertThat(member.getSchoolId()).isEqualTo(1L);
        assertThat(member.getProfileImageId()).isEqualTo("profile-file-id");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Nested
    @DisplayName("changeEmail")
    class ChangeEmail {

        @Test
        @DisplayName("활성 회원은 이메일을 변경할 수 있다")
        void changeEmail_activeMember_success() {
            Member member = Member.create("홍길동", "길동", "old@example.com", 1L, null);

            member.changeEmail("new@example.com");

            assertThat(member.getEmail()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("비활성 회원은 이메일을 변경할 수 없다")
        void changeEmail_inactiveMember_rejected() {
            Member member = Member.create("홍길동", "길동", "old@example.com", 1L, null);
            ReflectionTestUtils.setField(member, "status", MemberStatus.INACTIVE);

            assertThatThrownBy(() -> member.changeEmail("new@example.com"))
                .isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("nickname과 profileImageId를 부분 수정한다")
        void nickname과_profileImageId를_부분_수정한다() {
            Member member = Member.create("홍길동", "길동", "gildong@example.com", 1L, "old-file-id");

            member.updateProfile("하늘", null);
            member.updateProfile(null, "two-arg-file-id");
            member.updateProfile("new-file-id");

            assertThat(member.getNickname()).isEqualTo("하늘");
            assertThat(member.getProfileImageId()).isEqualTo("new-file-id");
        }

        @Test
        @DisplayName("비활성 회원은 프로필을 수정할 수 없다")
        void 비활성_회원은_프로필을_수정할_수_없다() {
            Member member = Member.create("홍길동", "길동", "gildong@example.com", 1L, null);
            ReflectionTestUtils.setField(member, "status", MemberStatus.INACTIVE);

            assertThatThrownBy(() -> member.updateProfile("하늘", null))
                .isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
    }

    @Nested
    @DisplayName("profile association")
    class ProfileAssociation {

        @Test
        @DisplayName("프로필을 할당하고 제거할 수 있다")
        void 프로필을_할당하고_제거할_수_있다() {
            Member member = Member.create("홍길동", "길동", "gildong@example.com", 1L, null);
            MemberProfile profile = MemberProfile.fromLinks(
                java.util.List.of(new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "https://github.com/umc"))
            );

            member.assignProfile(profile);
            assertThat(member.getProfile()).isSameAs(profile);

            member.removeProfile();
            assertThat(member.getProfile()).isNull();
        }

        @Test
        @DisplayName("비활성 회원은 프로필을 할당할 수 없다")
        void 비활성_회원은_프로필을_할당할_수_없다() {
            Member member = Member.create("홍길동", "길동", "gildong@example.com", 1L, null);
            ReflectionTestUtils.setField(member, "status", MemberStatus.INACTIVE);
            MemberProfile profile = MemberProfile.fromLinks(java.util.List.of());

            assertThatThrownBy(() -> member.assignProfile(profile))
                .isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
    }
}
