package com.umc.product.schedule.application.service.evaluator;

import static com.umc.product.support.fixture.ScheduleUnitFixture.participant;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendancePermissionEvaluator")
class AttendancePermissionEvaluatorTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;
    private static final Long GISU_ID = 100L;

    @Mock LoadSchedulePort loadSchedulePort;
    @Mock LoadScheduleParticipantPort loadScheduleParticipantPort;
    @Mock GetGisuUseCase getGisuUseCase;

    AttendancePermissionEvaluator sut;

    @BeforeEach
    void setUp() {
        sut = new AttendancePermissionEvaluator(loadSchedulePort, loadScheduleParticipantPort, getGisuUseCase);
    }

    @Test
    @DisplayName("지원 리소스 타입은 ATTENDANCE다")
    void supports_attendance_resource() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.ATTENDANCE);
    }

    @Test
    @DisplayName("WRITE는 resource ID·챌린저 활동·일정 참여를 모두 요구한다")
    void write_requires_resource_challenger_and_participant() {
        ResourcePermission withoutId = ResourcePermission.ofType(ResourceType.ATTENDANCE, PermissionType.WRITE);
        assertThat(sut.evaluate(challengerSubject(), withoutId)).isFalse();

        ResourcePermission withId = permission(PermissionType.WRITE);
        assertThat(sut.evaluate(subject(List.of(), List.of(), Set.of()), withId)).isFalse();

        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, MEMBER_ID))
            .willReturn(Optional.of(participant(schedule())));
        assertThat(sut.evaluate(challengerSubject(), withId)).isTrue();
        given(loadScheduleParticipantPort.findByScheduleIdAndMemberId(SCHEDULE_ID, MEMBER_ID))
            .willReturn(Optional.empty());
        assertThat(sut.evaluate(challengerSubject(), withId)).isFalse();
    }

    @Test
    @DisplayName("APPROVE는 resource ID와 해당 일정 기수의 운영진 역할을 요구한다")
    void approve_requires_target_gisu_admin() {
        assertThat(sut.evaluate(adminSubject(GISU_ID),
            ResourcePermission.ofType(ResourceType.ATTENDANCE, PermissionType.APPROVE))).isFalse();
        prepareTargetSchedule();
        assertThat(sut.evaluate(adminSubject(GISU_ID), permission(PermissionType.APPROVE))).isTrue();
        assertThat(sut.evaluate(adminSubject(GISU_ID + 1), permission(PermissionType.APPROVE))).isFalse();
        assertThat(sut.evaluate(subject(List.of(), List.of(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, GISU_ID)
        ), Set.of()), permission(PermissionType.APPROVE))).isFalse();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 일정별 APPROVE와 READ를 전역 override한다")
    void super_admin_is_global_override() {
        SubjectAttributes superAdmin = subject(List.of(), List.of(), Set.of(SystemRoleType.SUPER_ADMIN));

        assertThat(sut.evaluate(superAdmin, permission(PermissionType.APPROVE))).isTrue();
        assertThat(sut.evaluate(superAdmin, permission(PermissionType.READ))).isTrue();
        assertThat(sut.evaluate(superAdmin,
            ResourcePermission.ofType(ResourceType.ATTENDANCE, PermissionType.READ))).isTrue();
    }

    @Test
    @DisplayName("READ 목록은 운영진 이력, 단건은 대상 기수 운영진을 확인한다")
    void read_scope_differs_between_list_and_item() {
        ResourcePermission listRead = ResourcePermission.ofType(ResourceType.ATTENDANCE, PermissionType.READ);
        assertThat(sut.evaluate(adminSubject(GISU_ID + 1), listRead)).isTrue();
        assertThat(sut.evaluate(subject(List.of(), List.of(), Set.of()), listRead)).isFalse();

        prepareTargetSchedule();
        assertThat(sut.evaluate(adminSubject(GISU_ID), permission(PermissionType.READ))).isTrue();
        assertThat(sut.evaluate(adminSubject(GISU_ID + 1), permission(PermissionType.READ))).isFalse();
    }

    @Test
    @DisplayName("일정별 권한 평가에서 일정이 없으면 도메인 not-found를 반환한다")
    void target_schedule_not_found() {
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.evaluate(adminSubject(GISU_ID), permission(PermissionType.APPROVE)))
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND)
            );
    }

    @Test
    @DisplayName("지원하지 않는 permission은 fail-closed 처리한다")
    void unsupported_permission_is_denied() {
        ResourcePermission unsupported = mock(ResourcePermission.class);
        given(unsupported.permission()).willReturn(PermissionType.DELETE);

        assertThat(sut.evaluate(challengerSubject(), unsupported)).isFalse();
    }

    private void prepareTargetSchedule() {
        Schedule schedule = schedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(getGisuUseCase.getGisuByDate(schedule.getStartsAt())).willReturn(
            new GisuInfo(GISU_ID, 1L, Instant.EPOCH, Instant.parse("2031-01-01T00:00:00Z"), true)
        );
    }

    private SubjectAttributes challengerSubject() {
        return subject(List.of(GisuChallengerInfo.builder().gisuId(GISU_ID).challengerId(1L).build()),
            List.of(), Set.of());
    }

    private SubjectAttributes adminSubject(Long gisuId) {
        return subject(List.of(), List.of(role(ChallengerRoleType.SCHOOL_PART_LEADER, gisuId)), Set.of());
    }

    private SubjectAttributes subject(
        List<GisuChallengerInfo> challengers,
        List<RoleAttribute> roles,
        Set<SystemRoleType> systemRoles
    ) {
        return SubjectAttributes.builder()
            .memberId(MEMBER_ID)
            .gisuChallengerInfos(challengers)
            .roleAttributes(roles)
            .systemRoles(systemRoles)
            .build();
    }

    private RoleAttribute role(ChallengerRoleType type, Long gisuId) {
        return new RoleAttribute(type, OrganizationType.SCHOOL, 1L, null, gisuId);
    }

    private ResourcePermission permission(PermissionType type) {
        return ResourcePermission.of(ResourceType.ATTENDANCE, SCHEDULE_ID, type);
    }
}
