package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;

@ExtendWith(MockitoExtension.class)
class MemberSearchAccessScopeResolverTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    ListChallengerRoleUseCase listChallengerRoleUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    @InjectMocks
    MemberSearchAccessScopeResolver resolver;

    @Test
    @DisplayName("역할과 챌린저 이력이 없으면 검색 범위가 거부된다")
    void 역할과_챌린저_이력이_없으면_거부된다() {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of());
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(MEMBER_ID))).willReturn(Map.of());

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isTrue();
        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.allowedSchoolIds()).isEmpty();
        assertThat(scope.allowedGisuIds()).isEmpty();
        then(listChallengerRoleUseCase).should().listBasicByMemberId(MEMBER_ID);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(Set.of(MEMBER_ID));
        then(listChallengerRoleUseCase).shouldHaveNoMoreInteractions();
        then(getChallengerUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("챌린저 이력은 참여한 모든 기수만 허용한다")
    void 챌린저_이력은_참여한_모든_기수만_허용한다() {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of());
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID, List.of(challenger(10L, 3L), challenger(11L, 4L), challenger(12L, 3L))
        ));

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isFalse();
        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.allowedSchoolIds()).isEmpty();
        assertThat(scope.allowedGisuIds()).containsExactlyInAnyOrder(3L, 4L);
        then(listChallengerRoleUseCase).should().listBasicByMemberId(MEMBER_ID);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(Set.of(MEMBER_ID));
        then(listChallengerRoleUseCase).shouldHaveNoMoreInteractions();
        then(getChallengerUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("과거 학교 회장 역할은 해당 학교의 모든 기수를 허용한다")
    void 과거_학교_회장_역할은_해당_학교를_허용한다() {
        assertSchoolRoleGrantsSchool(ChallengerRoleType.SCHOOL_PRESIDENT);
    }

    @Test
    @DisplayName("과거 학교 부회장 역할은 해당 학교의 모든 기수를 허용한다")
    void 과거_학교_부회장_역할은_해당_학교를_허용한다() {
        assertSchoolRoleGrantsSchool(ChallengerRoleType.SCHOOL_VICE_PRESIDENT);
    }

    @Test
    @DisplayName("과거 중앙 총괄 역할은 챌린저 이력 없이도 무제한 범위를 허용한다")
    void 과거_중앙_총괄_역할은_무제한_범위를_허용한다() {
        assertCentralRoleGrantsUnrestricted(ChallengerRoleType.CENTRAL_PRESIDENT);
    }

    @Test
    @DisplayName("과거 중앙 부총괄 역할은 챌린저 이력 없이도 무제한 범위를 허용한다")
    void 과거_중앙_부총괄_역할은_무제한_범위를_허용한다() {
        assertCentralRoleGrantsUnrestricted(ChallengerRoleType.CENTRAL_VICE_PRESIDENT);
    }

    @Test
    @DisplayName("SUPER_ADMIN 역할은 챌린저 이력 없이도 무제한 범위를 허용한다")
    void superAdmin_역할은_무제한_범위를_허용한다() {
        // given
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of(
            new MemberSystemRoleInfo(MEMBER_ID, "SUPER_ADMIN")
        ));

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isFalse();
        assertThat(scope.unrestricted()).isTrue();
        assertThat(scope.allowedSchoolIds()).isEmpty();
        assertThat(scope.allowedGisuIds()).isEmpty();
        then(listMemberSystemRoleUseCase).should().listByMemberId(MEMBER_ID);
        then(listChallengerRoleUseCase).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("학교 범위와 챌린저 기수 범위 두 집합이 함께 보존된다")
    void 학교와_기수_범위_두_집합이_함께_보존된다() {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 7L)
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID, List.of(challenger(10L, 3L), challenger(11L, 4L))
        ));

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isFalse();
        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.allowedSchoolIds()).containsExactly(7L);
        assertThat(scope.allowedGisuIds()).containsExactlyInAnyOrder(3L, 4L);
        then(listChallengerRoleUseCase).should().listBasicByMemberId(MEMBER_ID);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(Set.of(MEMBER_ID));
        then(listChallengerRoleUseCase).shouldHaveNoMoreInteractions();
        then(getChallengerUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 학교와 기수 ID는 권한 범위에서 제외한다")
    void 유효하지_않은_학교와_기수_ID는_권한_범위에서_제외한다() {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, null),
            role(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, 0L),
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 7L)
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID, List.of(challenger(10L, null), challenger(11L, -1L), challenger(12L, 3L))
        ));

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isFalse();
        assertThat(scope.allowedSchoolIds()).containsExactly(7L);
        assertThat(scope.allowedGisuIds()).containsExactly(3L);
    }

    @Test
    @DisplayName("학교와 기수 ID가 모두 유효하지 않으면 검색 범위가 거부된다")
    void 학교와_기수_ID가_모두_유효하지_않으면_거부된다() {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, null),
            role(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, 0L)
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(MEMBER_ID))).willReturn(Map.of(
            MEMBER_ID, List.of(challenger(10L, null), challenger(11L, -1L))
        ));

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isTrue();
        assertThat(scope.allowedSchoolIds()).isEmpty();
        assertThat(scope.allowedGisuIds()).isEmpty();
    }

    @Test
    @DisplayName("제한 범위는 입력 Set을 방어적으로 복사하고 수정 불가능하게 노출한다")
    void 제한_범위는_입력_set을_방어적으로_복사한다() {
        // given
        Set<Long> schoolIds = new HashSet<>(Set.of(7L));
        Set<Long> gisuIds = new HashSet<>(Set.of(3L, 4L));

        // when
        MemberSearchAccessScope scope = MemberSearchAccessScope.restrictedTo(schoolIds, gisuIds);
        schoolIds.add(8L);
        gisuIds.clear();

        // then
        assertThat(scope.allowedSchoolIds()).containsExactly(7L);
        assertThat(scope.allowedGisuIds()).containsExactlyInAnyOrder(3L, 4L);
        assertThatThrownBy(() -> scope.allowedSchoolIds().add(9L))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("제한 범위 factory는 null Set과 유효하지 않은 ID를 거부한다")
    void 제한_범위_factory는_잘못된_입력을_거부한다() {
        // when & then
        assertThatThrownBy(() -> MemberSearchAccessScope.restrictedTo(null, Set.of()))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MemberSearchAccessScope.restrictedTo(Set.of(), Set.of(0L)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberSearchAccessScope.restrictedTo(Set.of(-1L), Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberSearchAccessScope.restrictedTo(Set.of(), Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertSchoolRoleGrantsSchool(ChallengerRoleType roleType) {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of(
            role(roleType, OrganizationType.SCHOOL, 7L)
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(MEMBER_ID))).willReturn(Map.of());

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isFalse();
        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.allowedSchoolIds()).containsExactly(7L);
        assertThat(scope.allowedGisuIds()).isEmpty();
        then(listChallengerRoleUseCase).should().listBasicByMemberId(MEMBER_ID);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(Set.of(MEMBER_ID));
        then(listChallengerRoleUseCase).shouldHaveNoMoreInteractions();
        then(getChallengerUseCase).shouldHaveNoMoreInteractions();
    }

    private void assertCentralRoleGrantsUnrestricted(ChallengerRoleType roleType) {
        // given
        given(listChallengerRoleUseCase.listBasicByMemberId(MEMBER_ID)).willReturn(List.of(
            role(roleType, OrganizationType.CENTRAL, null)
        ));

        // when
        MemberSearchAccessScope scope = resolver.resolve(MEMBER_ID);

        // then
        assertThat(scope.denied()).isFalse();
        assertThat(scope.unrestricted()).isTrue();
        assertThat(scope.allowedSchoolIds()).isEmpty();
        assertThat(scope.allowedGisuIds()).isEmpty();
        then(listChallengerRoleUseCase).should().listBasicByMemberId(MEMBER_ID);
        then(listChallengerRoleUseCase).shouldHaveNoMoreInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
    }

    private ChallengerRoleBasicInfo role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId
    ) {
        return new ChallengerRoleBasicInfo(roleType, organizationType, organizationId);
    }

    private ChallengerBasicInfo challenger(Long challengerId, Long gisuId) {
        return new ChallengerBasicInfo(challengerId, MEMBER_ID, gisuId, null, null);
    }
}
