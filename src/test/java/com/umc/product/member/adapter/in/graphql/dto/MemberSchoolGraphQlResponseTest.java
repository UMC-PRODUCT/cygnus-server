package com.umc.product.member.adapter.in.graphql.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

@DisplayName("MemberSchoolGraphQlResponse")
class MemberSchoolGraphQlResponseTest {

    @Test
    @DisplayName("null 링크와 시간은 빈 목록과 null 문자열로 변환한다")
    void null_필드를_변환한다() {
        MemberSchoolGraphQlResponse result = MemberSchoolGraphQlResponse.from(
            new SchoolDetailInfo(1L, "서울", "테스트대학교", 10L, null, null, null, true, null, null));

        assertThat(result.links()).isEmpty();
        assertThat(result.createdAt()).isNull();
        assertThat(result.updatedAt()).isNull();
    }

    @Test
    @DisplayName("학교 링크와 시간을 GraphQL 응답 형식으로 변환한다")
    void 링크와_시간을_변환한다() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        SchoolDetailInfo.SchoolLinkItem link = new SchoolDetailInfo.SchoolLinkItem(
            "인스타그램", SchoolLinkType.INSTAGRAM, "https://instagram.com/school");

        MemberSchoolGraphQlResponse result = MemberSchoolGraphQlResponse.from(
            new SchoolDetailInfo(
                1L, "서울", "테스트대학교", 10L, null, null,
                List.of(link), true, createdAt, createdAt));

        assertThat(result.links()).singleElement()
            .satisfies(response -> {
                assertThat(response.title()).isEqualTo("인스타그램");
                assertThat(response.type()).isEqualTo(SchoolLinkType.INSTAGRAM);
                assertThat(response.url()).isEqualTo("https://instagram.com/school");
            });
        assertThat(result.createdAt()).isEqualTo(createdAt.toString());
    }
}
