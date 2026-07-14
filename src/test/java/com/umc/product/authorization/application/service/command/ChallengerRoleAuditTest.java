package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Map;

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
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditSource;
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
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("챌린저 역할 rich audit")
class ChallengerRoleAuditTest {

    private static final long ROLE_ID = 301L;
    private static final long CHALLENGER_ID = 201L;
    private static final long TARGET_MEMBER_ID = 101L;
    private static final long ACTOR_MEMBER_ID = 102L;

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
    @DisplayName("기존 역할 부여는 역할을 저장하고 ID를 반환한다")
    void keepsExistingCreateBehavior() {
        givenRoleTarget();
        given(saveChallengerRolePort.save(any(ChallengerRole.class)))
            .willAnswer(invocation -> withId(invocation.getArgument(0)));

        Long roleId = sut.createChallengerRole(createCommand());

        assertThat(roleId).isEqualTo(ROLE_ID);
        then(saveChallengerRolePort).should().save(any(ChallengerRole.class));
    }

    @Test
    @DisplayName("역할 부여는 대상 회원과 역할명 snapshot을 발행한다")
    void publishesTargetAndRoleSnapshotOnCreate() {
        givenRoleTarget();
        given(saveChallengerRolePort.save(any(ChallengerRole.class)))
            .willAnswer(invocation -> withId(invocation.getArgument(0)));

        sut.createChallengerRole(createCommand());

        RecordAuditLogCommand command = capturedCommand();
        assertRoleCommand(command, AuditAction.CREATE, "SCHOOL_PRESIDENT");
        assertThat(section(command, "before")).isEmpty();
        assertThat(section(command, "after")).containsEntry("roleName", "SCHOOL_PRESIDENT");
    }

    @Test
    @DisplayName("역할 변경은 이전 및 이후 역할명 snapshot을 발행한다")
    void publishesBeforeAndAfterRoleSnapshotOnUpdate() {
        ChallengerRole role = role();
        given(loadChallengerRolePort.getById(ROLE_ID)).willReturn(role);
        givenRoleTarget();

        sut.updateChallengerRole(updateCommand(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, 10L));

        RecordAuditLogCommand command = capturedCommand();
        assertRoleCommand(command, AuditAction.UPDATE, "SCHOOL_VICE_PRESIDENT");
        assertThat(section(command, "before")).containsEntry("roleName", "SCHOOL_PRESIDENT");
        assertThat(section(command, "after")).containsEntry("roleName", "SCHOOL_VICE_PRESIDENT");
        then(saveChallengerRolePort).should().save(role);
    }

    @Test
    @DisplayName("역할 해제는 삭제 전 대상 회원과 역할명 snapshot을 발행한다")
    void publishesBeforeSnapshotOnDelete() {
        ChallengerRole role = role();
        given(loadChallengerRolePort.getById(ROLE_ID)).willReturn(role);
        givenRoleTarget();

        sut.deleteChallengerRole(DeleteChallengerRoleCommand.builder()
            .challengerRoleId(ROLE_ID)
            .actorMemberId(ACTOR_MEMBER_ID)
            .build());

        RecordAuditLogCommand command = capturedCommand();
        assertRoleCommand(command, AuditAction.DELETE, "SCHOOL_PRESIDENT");
        assertThat(section(command, "before")).containsEntry("roleName", "SCHOOL_PRESIDENT");
        assertThat(section(command, "after")).isEmpty();
        then(saveChallengerRolePort).should().delete(role);
    }

    @Test
    @DisplayName("잘못된 역할 변경은 저장하거나 성공 감사 이벤트를 발행하지 않는다")
    void doesNotPublishSuccessAuditOnInvalidRoleUpdate() {
        ChallengerRole role = role();
        given(loadChallengerRolePort.getById(ROLE_ID)).willReturn(role);
        givenRoleTarget();

        assertThatThrownBy(() -> sut.updateChallengerRole(
            updateCommand(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, null)
        )).isInstanceOf(IllegalArgumentException.class);

        then(saveChallengerRolePort).should(never()).save(any());
        then(recordAuditLogUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("대상 챌린저가 없으면 역할 부여와 성공 감사 이벤트를 만들지 않는다")
    void doesNotCreateRoleOrAuditWhenChallengerMissing() {
        given(getChallengerUseCase.getById(CHALLENGER_ID))
            .willThrow(new IllegalArgumentException("missing challenger"));

        assertThatThrownBy(() -> sut.createChallengerRole(createCommand()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("missing challenger");

        then(saveChallengerRolePort).shouldHaveNoInteractions();
        then(recordAuditLogUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("대상 회원이 없으면 역할 부여와 성공 감사 이벤트를 만들지 않는다")
    void doesNotCreateRoleOrAuditWhenTargetMemberMissing() {
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(challenger());
        given(getMemberUseCase.getById(TARGET_MEMBER_ID))
            .willThrow(new IllegalArgumentException("missing member"));

        assertThatThrownBy(() -> sut.createChallengerRole(createCommand()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("missing member");

        then(saveChallengerRolePort).shouldHaveNoInteractions();
        then(recordAuditLogUseCase).shouldHaveNoInteractions();
    }

    private void givenRoleTarget() {
        given(getChallengerUseCase.getById(CHALLENGER_ID)).willReturn(challenger());
        given(getMemberUseCase.getById(TARGET_MEMBER_ID)).willReturn(targetMember());
        given(getMemberUseCase.getById(ACTOR_MEMBER_ID)).willReturn(actorMember());
    }

    private CreateChallengerRoleCommand createCommand() {
        return CreateChallengerRoleCommand.builder()
            .challengerId(CHALLENGER_ID)
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(10L)
            .gisuId(1L)
            .actorMemberId(ACTOR_MEMBER_ID)
            .build();
    }

    private UpdateChallengerRoleCommand updateCommand(ChallengerRoleType roleType, Long organizationId) {
        return UpdateChallengerRoleCommand.builder()
            .challengerRoleId(ROLE_ID)
            .roleType(roleType)
            .organizationId(organizationId)
            .actorMemberId(ACTOR_MEMBER_ID)
            .build();
    }

    private static ChallengerRole role() {
        return withId(ChallengerRole.create(
            CHALLENGER_ID,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            10L,
            null,
            1L
        ));
    }

    private static ChallengerRole withId(ChallengerRole role) {
        ReflectionTestUtils.setField(role, "id", ROLE_ID);
        return role;
    }

    private static ChallengerInfo challenger() {
        return ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID)
            .memberId(TARGET_MEMBER_ID)
            .gisuId(1L)
            .build();
    }

    private static MemberInfo targetMember() {
        return MemberInfo.builder()
            .id(TARGET_MEMBER_ID)
            .name("역할대상")
            .nickname("대상닉네임")
            .email("must-not-persist@example.com")
            .schoolName("테스트대학교")
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private static MemberInfo actorMember() {
        return MemberInfo.builder()
            .id(ACTOR_MEMBER_ID)
            .name("역할관리자")
            .nickname("관리자닉네임")
            .email("actor-must-not-persist@example.com")
            .schoolName("관리자대학교")
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private RecordAuditLogCommand capturedCommand() {
        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should().record(captor.capture());
        return captor.getValue();
    }

    private static void assertRoleCommand(
        RecordAuditLogCommand command,
        AuditAction action,
        String roleName
    ) {
        assertThat(command.action()).isEqualTo(action);
        assertThat(command.targetType()).isEqualTo("ChallengerRole");
        assertThat(command.targetId()).isEqualTo(String.valueOf(ROLE_ID));
        assertThat(command.actorMemberId()).isEqualTo(ACTOR_MEMBER_ID);
        assertThat(command.source()).isEqualTo(AuditSource.EXPLICIT_RECORDER);
        assertThat(command.details()).containsEntry("schemaVersion", 1);
        assertThat(section(command, "actor"))
            .containsEntry("type", "Member")
            .containsEntry("memberId", ACTOR_MEMBER_ID)
            .containsEntry("name", "역할관리자")
            .containsEntry("nickname", "관리자닉네임")
            .containsEntry("schoolName", "관리자대학교");
        assertThat(section(command, "target"))
            .containsEntry("type", "Member")
            .containsEntry("id", ROLE_ID)
            .containsEntry("memberId", TARGET_MEMBER_ID)
            .containsEntry("name", "역할대상")
            .containsEntry("nickname", "대상닉네임")
            .containsEntry("schoolName", "테스트대학교")
            .containsEntry("status", "ACTIVE")
            .containsEntry("roleName", roleName);
        assertThat(command.details().toString())
            .doesNotContain(
                "email",
                "must-not-persist@example.com",
                "actor-must-not-persist@example.com",
                "providerId",
                "token",
                "subject"
            );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(RecordAuditLogCommand command, String name) {
        return (Map<String, Object>) command.details().get(name);
    }
}
