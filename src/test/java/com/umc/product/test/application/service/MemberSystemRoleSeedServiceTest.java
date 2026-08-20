package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;
import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleResult;
import com.umc.product.test.application.port.out.AssignSeedMemberSystemRolePort;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSystemRoleSeedService")
class MemberSystemRoleSeedServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock private CheckMemberExistenceUseCase checkMemberExistenceUseCase;
    @Mock private AssignSeedMemberSystemRolePort assignSeedMemberSystemRolePort;
    @Mock private EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @InjectMocks private MemberSystemRoleSeedService sut;

    @Test
    @DisplayName("존재하는 회원에게 SUPER_ADMIN 역할을 새로 부여한다")
    void createSuperAdminRole() {
        // given
        given(checkMemberExistenceUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(assignSeedMemberSystemRolePort.assignIfAbsent(
            MEMBER_ID, MemberSystemRoleType.SUPER_ADMIN)).willReturn(true);

        // when
        CreateSeedMemberSystemRoleResult result = sut.create(command());

        // then
        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.roleType()).isEqualTo(MemberSystemRoleType.SUPER_ADMIN);
        assertThat(result.created()).isTrue();
        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("이미 같은 역할이 있으면 중복 행 없이 권한 캐시만 제거한다")
    void keepExistingSuperAdminRole() {
        // given
        given(checkMemberExistenceUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(assignSeedMemberSystemRolePort.assignIfAbsent(
            MEMBER_ID, MemberSystemRoleType.SUPER_ADMIN)).willReturn(false);

        // when
        CreateSeedMemberSystemRoleResult result = sut.create(command());

        // then
        assertThat(result.created()).isFalse();
        then(evictAuthoritySnapshotCacheUseCase).should().evictByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 회원에게는 시스템 역할을 부여하지 않는다")
    void rejectMissingMember() {
        // given
        given(checkMemberExistenceUseCase.existsById(MEMBER_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.create(command()))
            .isInstanceOf(MemberDomainException.class);
        then(assignSeedMemberSystemRolePort).should(never())
            .assignIfAbsent(MEMBER_ID, MemberSystemRoleType.SUPER_ADMIN);
        then(evictAuthoritySnapshotCacheUseCase).shouldHaveNoInteractions();
    }

    private CreateSeedMemberSystemRoleCommand command() {
        return new CreateSeedMemberSystemRoleCommand(MEMBER_ID, MemberSystemRoleType.SUPER_ADMIN);
    }
}
