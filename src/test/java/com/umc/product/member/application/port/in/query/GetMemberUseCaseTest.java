package com.umc.product.member.application.port.in.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.service.MemberQueryService;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GetMemberUseCaseTest {

    @Mock
    LoadMemberPort loadMemberPort;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    MemberQueryService memberQueryService;

    private Member createMember(Long id, Long schoolId, String profileImageId) {
        Member member = Member.builder()
            .name("홍길동")
            .nickname("길동")
            .email("test@example.com")
            .schoolId(schoolId)
            .profileImageId(profileImageId)
            .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private SchoolDetailInfo createSchoolDetailInfo(Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            1L,           // chapterId
            "cassiopeia",    // chapterName
            schoolName,
            null,         // shortName
            schoolId,
            null,         // remark
            null,         // logoImageUrl
            List.of(),  // links
            true,         // isActive
            Instant.now(),
            Instant.now()
        );
    }

    // ── 헬퍼 메서드 ──

    private FileInfo createFileInfo(String fileId, String fileLink) {
        return new FileInfo(
            fileId,
            "profile.jpg",
            null,         // category
            "image/jpeg",
            1024L,
            fileLink,
            true,
            1L,
            Instant.now()
        );
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @Disabled
        void 회원_조회_성공() {
            // given
            Member member = createMember(1L, 1L, "profile_img_1");
            given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));

            // when
            MemberInfo result = memberQueryService.getById(1L);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.nickname()).isEqualTo("길동");
            assertThat(result.email()).isEqualTo("test@example.com");
        }

        @Test
        void 회원이_존재하지_않으면_예외() {
            // given
            given(loadMemberPort.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberQueryService.getById(999L))
                .isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("학교, 프로필 이미지, 역할 모두 있을 때 프로필 조회 성공")
        void 프로필_조회_성공_모든_정보_있음() {
            // given
            Member member = createMember(1L, 1L, "profile_img_1");
            given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));
            given(getSchoolUseCase.getSchoolDetail(1L))
                .willReturn(createSchoolDetailInfo(1L, "한양대학교ERICA"));
            given(getFileUseCase.getById("profile_img_1"))
                .willReturn(createFileInfo("profile_img_1", "https://cdn.example.com/profile.jpg"));
            given(getChallengerRoleUseCase.findAllByMemberId(1L)).willReturn(List.of());

            // when
            MemberInfo result = memberQueryService.getById(1L);

            // then
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.schoolName()).isEqualTo("한양대학교ERICA");
            assertThat(result.profileImageLink()).isEqualTo("https://cdn.example.com/profile.jpg");
            assertThat(result.roles()).isEmpty();
        }

        @Test
        @DisplayName("schoolId가 null이면 학교 조회를 하지 않는다")
        @Disabled
        void 프로필_조회_성공_학교ID가_null() {
            // given
            Member member = createMember(1L, null, "profile_img_1");
            given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));
            given(getFileUseCase.getById("profile_img_1"))
                .willReturn(createFileInfo("profile_img_1", "https://cdn.example.com/profile.jpg"));
            given(getChallengerRoleUseCase.findAllByMemberId(1L)).willReturn(List.of());

            // when
            MemberInfo result = memberQueryService.getById(1L);

            // then
            assertThat(result.schoolName()).isNull();
            then(getSchoolUseCase).should(never()).getSchoolDetail(any());
        }

        @Test
        @DisplayName("profileImageId가 null이면 파일 조회를 하지 않는다")
        void 프로필_조회_성공_프로필이미지ID가_null() {
            // given
            Member member = createMember(1L, 1L, null);
            given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));
            given(getSchoolUseCase.getSchoolDetail(1L))
                .willReturn(createSchoolDetailInfo(1L, "한양대학교ERICA"));
            given(getChallengerRoleUseCase.findAllByMemberId(1L)).willReturn(List.of());

            // when
            MemberInfo result = memberQueryService.getById(1L);

            // then
            assertThat(result.profileImageLink()).isNull();
            then(getFileUseCase).should(never()).getById(any());
        }

        @Test
        @DisplayName("schoolId와 profileImageId 모두 null이면 둘 다 조회하지 않는다")
        @Disabled
        void 프로필_조회_성공_학교와_이미지_모두_null() {
            // given
            Member member = createMember(1L, null, null);
            given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));
            given(getChallengerRoleUseCase.findAllByMemberId(1L)).willReturn(List.of());

            // when
            MemberInfo result = memberQueryService.getById(1L);

            // then
            assertThat(result.schoolName()).isNull();
            assertThat(result.profileImageLink()).isNull();
            then(getSchoolUseCase).should(never()).getSchoolDetail(any());
            then(getFileUseCase).should(never()).getById(any());
        }

        @Test
        void 회원이_존재하지_않으면_예외() {
            // given
            given(loadMemberPort.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberQueryService.getById(999L))
                .isInstanceOf(MemberDomainException.class)
                .extracting("baseCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);

            then(getSchoolUseCase).should(never()).getSchoolDetail(any());
            then(getFileUseCase).should(never()).getById(any());
        }

        @Test
        @Disabled
        @DisplayName("getSchoolDetail이 null을 반환하면 schoolName은 null이다")
        void 프로필_조회_학교정보가_null_반환() {
            // given
            Member member = createMember(1L, 1L, null);
            given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));
            given(getSchoolUseCase.getSchoolDetail(1L)).willReturn(null);
            given(getChallengerRoleUseCase.findAllByMemberId(1L)).willReturn(List.of());

            // when
            MemberInfo result = memberQueryService.getById(1L);

            // then
            assertThat(result.schoolName()).isNull();
        }
    }
}
