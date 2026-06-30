package com.umc.product.authorization.application.service.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.DeleteChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.UpdateChallengerRoleCommand;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheNamespace;

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
    CacheUseCase cacheUseCase;

    @Test
    @DisplayName("역할 생성 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_create() {
        ChallengerRoleCommandService sut = new ChallengerRoleCommandService(
            loadChallengerRolePort,
            saveChallengerRolePort,
            getChallengerUseCase,
            cacheUseCase
        );
        given(saveChallengerRolePort.save(any(ChallengerRole.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID)
            .memberId(MEMBER_ID)
            .build());

        sut.createChallengerRole(CreateChallengerRoleCommand.builder()
            .challengerId(CHALLENGER_ID)
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(SCHOOL_ID)
            .gisuId(GISU_ID)
            .build());

        verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:" + MEMBER_ID));
    }

    @Test
    @DisplayName("역할 수정 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_update() {
        ChallengerRoleCommandService sut = new ChallengerRoleCommandService(
            loadChallengerRolePort,
            saveChallengerRolePort,
            getChallengerUseCase,
            cacheUseCase
        );
        ChallengerRole role = ChallengerRole.create(
            CHALLENGER_ID,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.getById(CHALLENGER_ROLE_ID)).willReturn(role);
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID)
            .memberId(MEMBER_ID)
            .build());

        sut.updateChallengerRole(UpdateChallengerRoleCommand.builder()
            .challengerRoleId(CHALLENGER_ROLE_ID)
            .roleType(ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .organizationId(SCHOOL_ID)
            .build());

        verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:" + MEMBER_ID));
    }

    @Test
    @DisplayName("역할 삭제 후 해당 회원의 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_delete() {
        ChallengerRoleCommandService sut = new ChallengerRoleCommandService(
            loadChallengerRolePort,
            saveChallengerRolePort,
            getChallengerUseCase,
            cacheUseCase
        );
        ChallengerRole role = ChallengerRole.create(
            CHALLENGER_ID,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.getById(CHALLENGER_ROLE_ID)).willReturn(role);
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID)
            .memberId(MEMBER_ID)
            .build());

        sut.deleteChallengerRole(DeleteChallengerRoleCommand.builder()
            .challengerRoleId(CHALLENGER_ROLE_ID)
            .build());

        verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:" + MEMBER_ID));
    }
}
