package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("챌린저 역할 bulk audit 조회")
class ChallengerRoleBulkAuditTest {

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;
    @Mock
    SaveChallengerRolePort saveChallengerRolePort;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;
    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;
    @InjectMocks
    ChallengerRoleCommandService sut;

    @Test
    @DisplayName("bulk 역할 부여는 챌린저와 회원을 각각 IN query 한 번으로 조회한다")
    void bulkCreateLoadsAuditSnapshotsWithTwoBatchQueries() {
        List<CreateChallengerRoleCommand> commands = List.of(command(201L), command(202L));
        Set<Long> challengerIds = Set.of(201L, 202L);
        Set<Long> memberIds = Set.of(101L, 102L, 900L);
        given(getChallengerUseCase.batchGetByIds(challengerIds)).willReturn(List.of(
            challenger(201L, 101L),
            challenger(202L, 102L)
        ));
        given(getMemberUseCase.batchGetByIds(memberIds)).willReturn(Map.of(
            101L, member(101L, "대상일"),
            102L, member(102L, "대상이"),
            900L, member(900L, "운영자")
        ));
        given(saveChallengerRolePort.saveAll(anyList())).willAnswer(invocation -> {
            List<ChallengerRole> roles = invocation.getArgument(0);
            ReflectionTestUtils.setField(roles.get(0), "id", 301L);
            ReflectionTestUtils.setField(roles.get(1), "id", 302L);
            return roles;
        });

        assertThat(sut.createChallengerRoleBulk(commands)).containsExactly(301L, 302L);

        then(getChallengerUseCase).should().batchGetByIds(challengerIds);
        then(getMemberUseCase).should().batchGetByIds(memberIds);
        then(getChallengerUseCase).should(never()).getById(anyLong());
        then(getMemberUseCase).should(never()).getById(anyLong());
        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should(times(2)).record(captor.capture());
        assertThat(captor.getAllValues()).extracting(RecordAuditLogCommand::targetId)
            .containsExactly("301", "302");
    }

    private CreateChallengerRoleCommand command(Long challengerId) {
        return CreateChallengerRoleCommand.builder()
            .challengerId(challengerId)
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(10L)
            .gisuId(1L)
            .actorMemberId(900L)
            .build();
    }

    private ChallengerInfo challenger(Long challengerId, Long memberId) {
        return ChallengerInfo.builder()
            .challengerId(challengerId)
            .memberId(memberId)
            .gisuId(1L)
            .build();
    }

    private MemberInfo member(Long id, String name) {
        return MemberInfo.builder().id(id).name(name).nickname(name).schoolName("테스트대학교").build();
    }
}
