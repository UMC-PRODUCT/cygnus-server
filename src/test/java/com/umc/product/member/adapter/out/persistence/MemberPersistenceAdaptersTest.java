package com.umc.product.member.adapter.out.persistence;

import static com.umc.product.support.fixture.MemberUnitFixture.회원;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.MemberProfile;
import com.umc.product.member.domain.MemberSystemRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("Member persistence adapter 계약")
class MemberPersistenceAdaptersTest {

    @Mock
    MemberJpaRepository memberJpaRepository;
    @Mock
    MemberQueryRepository memberQueryRepository;
    @InjectMocks
    MemberPersistenceAdapter memberAdapter;

    @Test
    @DisplayName("단건·이메일·닉네임·lock 조회를 각 저장소에 위임한다")
    void 단건_조회를_위임한다() {
        Member member = 회원(1L, 10L);
        given(memberJpaRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberQueryRepository.findByIdWithPessimisticLock(1L)).willReturn(Optional.of(member));
        given(memberJpaRepository.findByEmail("member@example.com")).willReturn(Optional.of(member));
        given(memberQueryRepository.findByNickname("길동")).willReturn(Optional.of(member));

        assertThat(memberAdapter.findById(1L)).containsSame(member);
        assertThat(memberAdapter.findByIdForUpdate(1L)).containsSame(member);
        assertThat(memberAdapter.findByEmail("member@example.com")).containsSame(member);
        assertThat(memberAdapter.findByNickname("길동")).containsSame(member);
    }

    @Test
    @DisplayName("회원 ID와 학교 범위 조회를 위임하고 projection을 학교별 Set으로 묶는다")
    void 범위_조회를_변환한다() {
        Member member = 회원(1L, 10L);
        MemberJpaRepository.SchoolMemberIdRow first = row(10L, 1L);
        MemberJpaRepository.SchoolMemberIdRow second = row(10L, 2L);
        MemberJpaRepository.SchoolMemberIdRow third = row(20L, 3L);
        given(memberJpaRepository.findAllById(Set.of(1L))).willReturn(List.of(member));
        given(memberJpaRepository.findAllIdsBySchoolId(10L)).willReturn(Set.of(1L, 2L));
        given(memberJpaRepository.findAllIdsBySchoolIds(Set.of(10L, 20L)))
            .willReturn(List.of(first, second, third));

        assertThat(memberAdapter.findAllByIds(Set.of(1L))).containsExactly(member);
        assertThat(memberAdapter.listIdsBySchoolId(10L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(memberAdapter.listIdsBySchoolIds(Set.of(10L, 20L)))
            .isEqualTo(Map.of(10L, Set.of(1L, 2L), 20L, Set.of(3L)));
    }

    @Test
    @DisplayName("학교 ID 집합이 null 또는 비어 있으면 projection 조회를 생략한다")
    void 빈_학교_범위를_처리한다() {
        assertThat(memberAdapter.listIdsBySchoolIds(null)).isEmpty();
        assertThat(memberAdapter.listIdsBySchoolIds(Set.of())).isEmpty();
        then(memberJpaRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재 여부·저장·일괄 저장·삭제를 JPA 저장소에 위임한다")
    void 기본_변경을_위임한다() {
        Member member = 회원(1L, 10L);
        given(memberJpaRepository.existsById(1L)).willReturn(true);
        given(memberJpaRepository.existsByEmail("member@example.com")).willReturn(true);
        given(memberJpaRepository.existsByNickname("길동")).willReturn(true);
        given(memberJpaRepository.save(member)).willReturn(member);
        given(memberJpaRepository.saveAll(List.of(member))).willReturn(List.of(member));

        assertThat(memberAdapter.existsById(1L)).isTrue();
        assertThat(memberAdapter.existsByEmail("member@example.com")).isTrue();
        assertThat(memberAdapter.existsByNickname("길동")).isTrue();
        assertThat(memberAdapter.save(member)).isSameAs(member);
        assertThat(memberAdapter.saveAll(List.of(member))).containsExactly(member);
        memberAdapter.delete(member);
        then(memberJpaRepository).should().delete(member);
    }

    @Test
    @DisplayName("검색·범위 검색·cursor 조회를 해당 저장소에 위임한다")
    @SuppressWarnings("unchecked")
    void 검색을_위임한다() {
        SearchMemberQuery query = mock(SearchMemberQuery.class);
        MemberSearchAccessScope scope = MemberSearchAccessScope.allowAll();
        Pageable pageable = mock(Pageable.class);
        Page<Challenger> challengers = mock(Page.class);
        Page<Long> memberIds = mock(Page.class);
        given(memberQueryRepository.searchBy(query, pageable)).willReturn(challengers);
        given(memberQueryRepository.searchMemberIdsBy(query, pageable)).willReturn(memberIds);
        given(memberQueryRepository.searchMemberIdsBy(query, scope, pageable)).willReturn(memberIds);
        given(memberJpaRepository.findIdsCursor(10L, pageable)).willReturn(List.of(11L));

        assertThat(memberAdapter.search(query, pageable)).isSameAs(challengers);
        assertThat(memberAdapter.searchMemberIds(query, pageable)).isSameAs(memberIds);
        assertThat(memberAdapter.searchMemberIds(query, scope, pageable)).isSameAs(memberIds);
        assertThat(memberAdapter.findAllIdsCursor(10L, pageable)).containsExactly(11L);
    }

    @Test
    @DisplayName("회원 수 집계는 빈 집합을 0으로 처리하고 그 외에는 저장소에 위임한다")
    void 회원_수를_집계한다() {
        given(memberJpaRepository.countByIdIn(Set.of(1L, 2L))).willReturn(2L);
        given(memberJpaRepository.count()).willReturn(10L);

        assertThat(memberAdapter.countMembersByIds(null)).isZero();
        assertThat(memberAdapter.countMembersByIds(Set.of())).isZero();
        assertThat(memberAdapter.countMembersByIds(Set.of(1L, 2L))).isEqualTo(2L);
        assertThat(memberAdapter.countAllMembers()).isEqualTo(10L);
    }

    @Test
    @DisplayName("프로필 adapter는 단건·집합 조회와 모든 변경을 위임한다")
    void 프로필_adapter를_검증한다() {
        MemberProfileJpaRepository repository = mock(MemberProfileJpaRepository.class);
        MemberProfilePersistenceAdapter adapter = new MemberProfilePersistenceAdapter(repository);
        MemberProfile profile = mock(MemberProfile.class);
        given(repository.findById(1L)).willReturn(Optional.of(profile));
        given(repository.findAllById(Set.of(1L))).willReturn(List.of(profile));
        given(repository.save(profile)).willReturn(profile);
        given(repository.saveAll(List.of(profile))).willReturn(List.of(profile));

        assertThat(adapter.findById(1L)).containsSame(profile);
        assertThat(adapter.findByIdIn(Set.of(1L))).containsExactly(profile);
        assertThat(adapter.save(profile)).isSameAs(profile);
        assertThat(adapter.saveAll(List.of(profile))).containsExactly(profile);
        adapter.delete(profile);
        then(repository).should().delete(profile);
    }

    @Test
    @DisplayName("시스템 역할 adapter는 회원의 역할 목록 조회를 위임한다")
    void 시스템_역할_adapter를_검증한다() {
        MemberSystemRoleJpaRepository repository = mock(MemberSystemRoleJpaRepository.class);
        MemberSystemRolePersistenceAdapter adapter = new MemberSystemRolePersistenceAdapter(repository);
        MemberSystemRole role = mock(MemberSystemRole.class);
        given(repository.findAllByMemberId(1L)).willReturn(List.of(role));

        assertThat(adapter.listByMemberId(1L)).containsExactly(role);
    }

    private MemberJpaRepository.SchoolMemberIdRow row(Long schoolId, Long memberId) {
        MemberJpaRepository.SchoolMemberIdRow row = mock(MemberJpaRepository.SchoolMemberIdRow.class);
        given(row.getSchoolId()).willReturn(schoolId);
        given(row.getMemberId()).willReturn(memberId);
        return row;
    }
}
