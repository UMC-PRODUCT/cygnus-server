package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberQueryService batch 조회")
class MemberQueryServiceBatchTest {

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

    @Test
    @DisplayName("findAllByIds는 빈 입력이면 외부 조회 없이 빈 Map을 반환한다")
    void findAllByIds는_빈_입력이면_외부_조회_없이_빈_Map을_반환한다() {
        assertThat(sut.findAllByIds(Set.of())).isEmpty();
        assertThat(sut.findAllByIds(null)).isEmpty();

        then(loadMemberPort).should(never()).findAllByIds(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("findAllByIds는 같은 schoolId와 profileImageId를 한 번만 조회한다")
    void findAllByIds는_같은_schoolId와_profileImageId를_한_번만_조회한다() {
        Member first = member(1L, "홍길동", 10L, "profile-file-id");
        Member second = member(2L, "김철수", 10L, "profile-file-id");
        given(loadMemberPort.findAllByIds(Set.of(1L, 2L))).willReturn(List.of(first, second));
        given(getSchoolUseCase.batchGetNamesByIds(Set.of(10L)))
            .willReturn(List.of(new SchoolNameInfo(10L, "테스트대학교")));
        given(getFileUseCase.findAllByIds(List.of("profile-file-id")))
            .willReturn(Map.of("profile-file-id", file("profile-file-id")));

        Map<Long, MemberInfo> result = sut.findAllByIds(Set.of(1L, 2L));

        assertThat(result).containsKeys(1L, 2L);
        assertThat(result.get(1L).schoolName()).isEqualTo("테스트대학교");
        assertThat(result.get(2L).profileImageLink()).isEqualTo("https://cdn.example.com/profile-file-id");
        then(getSchoolUseCase).should(times(1)).batchGetNamesByIds(Set.of(10L));
        then(getFileUseCase).should(times(1)).findAllByIds(List.of("profile-file-id"));
        then(getSchoolUseCase).should(never()).getSchoolDetail(anyLong());
        then(getFileUseCase).should(never()).getById(anyString());
    }

    @Test
    @DisplayName("findAllByIds는 서로 다른 학교와 프로필도 단건 조회를 반복하지 않는다")
    void findAllByIds는_학교와_프로필을_IN_query로_조회한다() {
        Member first = member(1L, "홍길동", 10L, "profile-1");
        Member second = member(2L, "김철수", 20L, "profile-2");
        given(loadMemberPort.findAllByIds(Set.of(1L, 2L))).willReturn(List.of(first, second));
        given(getSchoolUseCase.batchGetNamesByIds(Set.of(10L, 20L))).willReturn(List.of(
            new SchoolNameInfo(10L, "첫대학교"),
            new SchoolNameInfo(20L, "둘대학교")
        ));
        given(getFileUseCase.findAllByIds(List.of("profile-1", "profile-2"))).willReturn(Map.of(
            "profile-1", file("profile-1"),
            "profile-2", file("profile-2")
        ));

        Map<Long, MemberInfo> result = sut.findAllByIds(Set.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L).schoolName()).isEqualTo("첫대학교");
        assertThat(result.get(2L).profileImageLink()).endsWith("profile-2");
        then(getSchoolUseCase).should().batchGetNamesByIds(Set.of(10L, 20L));
        then(getFileUseCase).should().findAllByIds(List.of("profile-1", "profile-2"));
        then(getSchoolUseCase).should(never()).getSchoolDetail(anyLong());
        then(getFileUseCase).should(never()).getById(anyString());
    }

    @Test
    @DisplayName("findAllByIds는 profileImageId가 null이어도 빈 bulk 결과에서 안전하게 조립한다")
    void findAllByIds는_null_profileImageId를_안전하게_조립한다() {
        Member member = member(1L, "홍길동", null, null);
        given(loadMemberPort.findAllByIds(Set.of(1L))).willReturn(List.of(member));
        given(getSchoolUseCase.batchGetNamesByIds(Set.of())).willReturn(List.of());

        Map<Long, MemberInfo> result = sut.findAllByIds(Set.of(1L));

        assertThat(result.get(1L).profileImageLink()).isNull();
        then(getFileUseCase).should(never()).findAllByIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("listIdsBySchoolIds는 조회 결과가 없는 학교도 빈 Set으로 채운다")
    void listIdsBySchoolIds는_조회_결과가_없는_학교도_빈_Set으로_채운다() {
        given(loadMemberPort.listIdsBySchoolIds(Set.of(10L, 20L)))
            .willReturn(Map.of(10L, Set.of(1L, 2L)));

        Map<Long, Set<Long>> result = sut.listIdsBySchoolIds(Set.of(10L, 20L));

        assertThat(result.get(10L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.get(20L)).isEmpty();
    }

    private Member member(Long id, String name, Long schoolId, String profileImageId) {
        Member member = Member.create(name, name.substring(0, 2), name + "@example.com", schoolId, profileImageId);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private FileInfo file(String fileId) {
        return new FileInfo(
            fileId,
            "profile.png",
            null,
            "image/png",
            100L,
            "https://cdn.example.com/" + fileId,
            true,
            1L,
            Instant.parse("2024-01-01T00:00:00Z")
        );
    }
}
