package com.umc.product.member.application.service;

import static com.umc.product.support.fixture.MemberUnitFixture.학교_상세;
import static com.umc.product.support.fixture.MemberUnitFixture.회원;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.domain.LinkTypeAndLink;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.MemberProfile;
import com.umc.product.member.domain.MemberProfileLinkType;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberQueryService 엣지 케이스")
class MemberQueryServiceEdgeCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHOOL_ID = 10L;

    @Mock
    LoadMemberPort loadMemberPort;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    MemberQueryService sut;

    @Nested
    @DisplayName("단건 조회")
    class SingleLookup {

        @Test
        @DisplayName("회원에 학교명·프로필 링크·역할을 결합한다")
        void 회원_정보를_결합한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            List<ChallengerRoleInfo> roles = List.of(ChallengerRoleInfo.builder().id(7L).build());
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(학교_상세(SCHOOL_ID, "테스트대학교"));
            given(getFileUseCase.getById("profile-image-id")).willReturn(file("profile-image-id"));
            given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(roles);

            MemberInfo result = sut.getById(MEMBER_ID);

            assertThat(result.schoolName()).isEqualTo("테스트대학교");
            assertThat(result.profileImageLink()).isEqualTo("https://cdn.example/profile-image-id");
            assertThat(result.roles()).isSameAs(roles);
        }

        @Test
        @DisplayName("프로필 이미지 ID가 없으면 storage를 조회하지 않고 null 링크를 반환한다")
        void 프로필_이미지가_없는_회원을_조회한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            ReflectionTestUtils.setField(member, "profileImageId", null);
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(학교_상세(SCHOOL_ID, "테스트대학교"));
            given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

            assertThat(sut.findByIdOrNull(MEMBER_ID).profileImageLink()).isNull();
            then(getFileUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 ID는 Optional·null·필수 get 계약에 맞게 처리한다")
        void 누락_회원_계약을_구분한다() {
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.empty());

            assertThat(sut.findById(MEMBER_ID)).isEmpty();
            assertThat(sut.findByIdOrNull(MEMBER_ID)).isNull();
            assertThatThrownBy(() -> sut.getById(MEMBER_ID))
                .isInstanceOfSatisfying(MemberDomainException.class, exception ->
                    assertThat(exception.getBaseCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND)
                );
        }

        @Test
        @DisplayName("회원 프로필이 없어도 빈 MemberProfileInfo를 반환한다")
        void 프로필_없음을_빈_dto로_변환한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));

            assertThat(sut.getMemberProfileById(MEMBER_ID).id()).isNull();
        }

        @Test
        @DisplayName("회원 프로필의 모든 링크 유형을 누락 없이 변환한다")
        void 프로필_링크를_변환한다() {
            Member member = 회원(MEMBER_ID, SCHOOL_ID);
            MemberProfile profile = MemberProfile.fromLinks(List.of(
                new LinkTypeAndLink(MemberProfileLinkType.LINKEDIN, "linkedin"),
                new LinkTypeAndLink(MemberProfileLinkType.INSTAGRAM, "instagram"),
                new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "github"),
                new LinkTypeAndLink(MemberProfileLinkType.BLOG, "blog"),
                new LinkTypeAndLink(MemberProfileLinkType.PERSONAL, "personal")
            ));
            ReflectionTestUtils.setField(profile, "id", 5L);
            member.assignProfile(profile);
            given(loadMemberPort.findById(MEMBER_ID)).willReturn(Optional.of(member));

            assertThat(sut.getMemberProfileById(MEMBER_ID))
                .satisfies(result -> {
                    assertThat(result.id()).isEqualTo(5L);
                    assertThat(result.linkedIn()).isEqualTo("linkedin");
                    assertThat(result.instagram()).isEqualTo("instagram");
                    assertThat(result.github()).isEqualTo("github");
                    assertThat(result.blog()).isEqualTo("blog");
                    assertThat(result.personal()).isEqualTo("personal");
                });
        }
    }

    @Nested
    @DisplayName("배치 및 위임 조회")
    class BatchAndDelegation {

        @Test
        @DisplayName("batch 회원의 school·profile 누락과 null school 상세를 graceful하게 처리한다")
        void batch_누락_정보를_처리한다() {
            Member withoutSchool = 회원(1L, null);
            ReflectionTestUtils.setField(withoutSchool, "profileImageId", null);
            Member missingSchoolDetail = 회원(2L, SCHOOL_ID);
            given(loadMemberPort.findAllByIds(Set.of(1L, 2L)))
                .willReturn(List.of(withoutSchool, missingSchoolDetail));
            given(getSchoolUseCase.getSchoolDetail(SCHOOL_ID)).willReturn(null);
            given(getFileUseCase.getById("profile-image-id")).willReturn(file("profile-image-id"));

            Map<Long, MemberInfo> result = sut.findAllByIds(Set.of(1L, 2L));

            assertThat(result.get(1L).schoolName()).isNull();
            assertThat(result.get(1L).profileImageLink()).isNull();
            assertThat(result.get(2L).schoolName()).isNull();
        }

        @Test
        @DisplayName("school ID 배치와 단건 조회는 null·empty 입력을 port 호출 없이 처리한다")
        void school_id_빈_입력을_처리한다() {
            assertThat(sut.findAllSchoolIdsByIds(null)).isEmpty();
            assertThat(sut.findAllSchoolIdsByIds(Set.of())).isEmpty();
            assertThat(sut.listIdsBySchoolId(null)).isEmpty();
            assertThat(sut.listIdsBySchoolIds(null)).isEmpty();
            assertThat(sut.listIdsBySchoolIds(Set.of())).isEmpty();
            then(loadMemberPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원별 school ID는 null school을 제외하고 반환한다")
        void null_school을_제외한다() {
            Member assigned = 회원(1L, SCHOOL_ID);
            Member unassigned = 회원(2L, null);
            given(loadMemberPort.findAllByIds(Set.of(1L, 2L))).willReturn(List.of(assigned, unassigned));

            assertThat(sut.findAllSchoolIdsByIds(Set.of(1L, 2L)))
                .containsExactly(Map.entry(1L, SCHOOL_ID));
        }

        @Test
        @DisplayName("존재·수·cursor 조회를 정확한 port 계약으로 위임한다")
        void 단순_조회를_port에_위임한다() {
            given(loadMemberPort.listIdsBySchoolId(SCHOOL_ID)).willReturn(Set.of(1L));
            given(loadMemberPort.existsById(MEMBER_ID)).willReturn(true);
            given(loadMemberPort.existsByEmail("member@example.com")).willReturn(true);
            given(loadMemberPort.findAllIdsCursor(5L, PageRequest.of(0, 3))).willReturn(List.of(6L, 7L));
            given(loadMemberPort.countMembersByIds(Set.of(1L, 2L))).willReturn(2L);
            given(loadMemberPort.countAllMembers()).willReturn(10L);

            assertThat(sut.listIdsBySchoolId(SCHOOL_ID)).containsExactly(1L);
            assertThat(sut.existsById(MEMBER_ID)).isTrue();
            assertThat(sut.existsByEmail("member@example.com")).isTrue();
            assertThat(sut.findAllIdsCursor(5L, 3)).containsExactly(6L, 7L);
            assertThat(sut.countMembersByIds(Set.of(1L, 2L))).isEqualTo(2L);
            assertThat(sut.countAll()).isEqualTo(10L);
        }
    }

    private FileInfo file(String fileId) {
        return new FileInfo(
            fileId,
            "profile.png",
            null,
            "image/png",
            100L,
            "https://cdn.example/" + fileId,
            true,
            MEMBER_ID,
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
