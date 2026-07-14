package com.umc.product.authorization.application.service.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.DeleteChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.UpdateChallengerRoleCommand;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRoleCommandService")
class ChallengerRoleCommandServiceTest {

    private static final Long CHALLENGER_ROLE_ID = 1L;
    private static final Long CHALLENGER_ID = 100L;
    private static final Long MEMBER_ID = 10L;
    private static final Long SCHOOL_ID = 30L;
    private static final Long GISU_ID = 9L;

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    SaveChallengerRolePort saveChallengerRolePort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;

    @Test
    @DisplayName("역할 생성 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_create() {
        ChallengerRoleCommandService sut = sut();
        given(saveChallengerRolePort.save(any(ChallengerRole.class))).willAnswer(invocation -> {
            ChallengerRole role = invocation.getArgument(0);
            ReflectionTestUtils.setField(role, "id", CHALLENGER_ROLE_ID);
            return role;
        });
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(challengerInfo());
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(memberInfo());

        sut.createChallengerRole(CreateChallengerRoleCommand.builder()
            .challengerId(CHALLENGER_ID)
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(SCHOOL_ID)
            .gisuId(GISU_ID)
            .build());

        verify(evictAuthoritySnapshotCacheUseCase).evictByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("역할을 대량 생성한 후 해당 회원들의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_bulk_create() {
        ChallengerRoleCommandService sut = sut();
        given(saveChallengerRolePort.saveAll(any())).willAnswer(invocation -> {
            List<ChallengerRole> roles = invocation.getArgument(0);
            ReflectionTestUtils.setField(roles.getFirst(), "id", CHALLENGER_ROLE_ID);
            return roles;
        });
        given(getChallengerUseCase.batchGetByIds(any())).willReturn(List.of(challengerInfo()));
        given(getMemberUseCase.batchGetByIds(any())).willReturn(Map.of(MEMBER_ID, memberInfo()));

        sut.createChallengerRoleBulk(List.of(CreateChallengerRoleCommand.builder()
            .challengerId(CHALLENGER_ID)
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(SCHOOL_ID)
            .gisuId(GISU_ID)
            .build()));

        verify(evictAuthoritySnapshotCacheUseCase).evictByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("역할 수정 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_update() {
        ChallengerRoleCommandService sut = sut();
        ChallengerRole role = ChallengerRole.create(
            CHALLENGER_ID,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        ReflectionTestUtils.setField(role, "id", CHALLENGER_ROLE_ID);
        given(loadChallengerRolePort.getById(CHALLENGER_ROLE_ID)).willReturn(role);
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(challengerInfo());
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(memberInfo());

        sut.updateChallengerRole(UpdateChallengerRoleCommand.builder()
            .challengerRoleId(CHALLENGER_ROLE_ID)
            .roleType(ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .organizationId(SCHOOL_ID)
            .build());

        verify(evictAuthoritySnapshotCacheUseCase).evictByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("역할 삭제 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_delete() {
        ChallengerRoleCommandService sut = sut();
        ChallengerRole role = ChallengerRole.create(
            CHALLENGER_ID,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        ReflectionTestUtils.setField(role, "id", CHALLENGER_ROLE_ID);
        given(loadChallengerRolePort.getById(CHALLENGER_ROLE_ID)).willReturn(role);
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(challengerInfo());
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(memberInfo());

        sut.deleteChallengerRole(DeleteChallengerRoleCommand.builder()
            .challengerRoleId(CHALLENGER_ROLE_ID)
            .build());

        verify(evictAuthoritySnapshotCacheUseCase).evictByMemberId(MEMBER_ID);
    }

    private ChallengerRoleCommandService sut() {
        return new ChallengerRoleCommandService(
            loadChallengerRolePort,
            saveChallengerRolePort,
            getChallengerUseCase,
            evictAuthoritySnapshotCacheUseCase,
            getMemberUseCase,
            recordAuditLogUseCase
        );
    }

    private ChallengerInfo challengerInfo() {
        return ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID)
            .memberId(MEMBER_ID)
            .build();
    }

    private MemberInfo memberInfo() {
        return MemberInfo.builder()
            .id(MEMBER_ID)
            .name("테스트 회원")
            .nickname("테스트")
            .schoolId(SCHOOL_ID)
            .schoolName("테스트 학교")
            .build();
    }
}
