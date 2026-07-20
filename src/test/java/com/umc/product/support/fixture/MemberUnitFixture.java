package com.umc.product.support.fixture;

import java.time.Instant;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

public final class MemberUnitFixture {

    private MemberUnitFixture() {
    }

    public static Member 회원(Long id, Long schoolId) {
        Member member = Member.create(
            "홍길동",
            "길동",
            "member@example.com",
            schoolId,
            "profile-image-id"
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static OAuthRegisterMemberCommand OAuth_회원가입_명령(
        String providerId,
        Long schoolId,
        List<TermConsents> termConsents
    ) {
        return OAuthRegisterMemberCommand.builder()
            .provider(OAuthProvider.APPLE)
            .providerId(providerId)
            .name("홍길동")
            .nickname("길동")
            .email(providerId + "@example.com")
            .schoolId(schoolId)
            .profileImageId("profile-image-id")
            .termConsents(termConsents)
            .appleRefreshToken("apple-refresh-token")
            .appleClientId("apple-client-id")
            .build();
    }

    public static List<TermConsents> 필수_약관_동의() {
        return List.of(new TermConsents(1L, true), new TermConsents(2L, true));
    }

    public static SchoolDetailInfo 학교_상세(Long schoolId, String schoolName) {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        return new SchoolDetailInfo(
            1L,
            "서울",
            schoolName,
            schoolId,
            null,
            null,
            List.of(),
            true,
            createdAt,
            createdAt
        );
    }
}
