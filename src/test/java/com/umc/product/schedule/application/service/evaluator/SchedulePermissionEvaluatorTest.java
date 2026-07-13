package com.umc.product.schedule.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.domain.Schedule;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulePermissionEvaluator")
class SchedulePermissionEvaluatorTest {

    private static final Long SCHEDULE_ID = 100L;
    private static final Long AUTHOR_MEMBER_ID = 10L;
    private static final Long SCHEDULE_GISU_ID = 1L;
    private static final Long OTHER_GISU_ID = 99L;
    @Mock
    LoadSchedulePort loadSchedulePort;
    @InjectMocks
    SchedulePermissionEvaluator sut;

    @Test
    @DisplayName("supportedResourceType은 SCHEDULE을 반환한다")
    void supportedResourceType은_SCHEDULE을_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.SCHEDULE);
    }

    @Test
    @DisplayName("챌린저 활동 기록이 없는 SUPER_ADMIN도 일정을 생성할 수 있다")
    void 챌린저_활동_기록이_없는_SUPER_ADMIN도_일정_생성_허용() {
        SubjectAttributes subject = superAdminSubject(20L);
        ResourcePermission permission = ResourcePermission.of(
            ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    private void givenSchedule() {
        Schedule schedule = schedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
    }

    private Schedule schedule() {
        Schedule schedule = new Schedule() {
        };
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
        ReflectionTestUtils.setField(schedule, "authorMemberId", AUTHOR_MEMBER_ID);
        ReflectionTestUtils.setField(schedule, "startsAt", Instant.parse("2026-05-13T10:00:00Z"));
        return schedule;
    }

    // --- helpers ---

    private SubjectAttributes subjectWith(Long memberId, List<RoleAttribute> roles) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(1L)
            .gisuChallengerInfos(List.<GisuChallengerInfo>of())
            .roleAttributes(roles)
            .build();
    }

    private SubjectAttributes superAdminSubject(Long memberId) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(1L)
            .gisuChallengerInfos(List.<GisuChallengerInfo>of())
            .roleAttributes(List.of())
            .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN))
            .build();
    }

    private RoleAttribute centralCoreRoleInGisu(Long gisuId) {
        return new RoleAttribute(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null, null, gisuId
        );
    }

    @Nested
    @DisplayName("DELETE - 일반 삭제 권한")
    class delete {

        @Test
        @DisplayName("일정 생성자 본인이면 허용")
        void 생성자_본인_허용() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(AUTHOR_MEMBER_ID, List.of());
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.DELETE);

            assertThat(sut.evaluate(subject, permission)).isTrue();
        }

        @Test
        @DisplayName("SUPER_ADMIN system role이면 허용")
        void SUPER_ADMIN_system_role_허용() {
            givenSchedule();

            SubjectAttributes subject = superAdminSubject(20L);
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.DELETE);

            assertThat(sut.evaluate(subject, permission)).isTrue();
        }

        @Test
        @DisplayName("생성자도 아니고 해당 기수 SUPER_ADMIN도 아니면 거부")
        void 생성자_아니고_SUPER_ADMIN_아니면_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(20L, List.of());
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }

        @Test
        @DisplayName("다른 기수의 중앙총괄이면 거부")
        void 다른_기수_중앙총괄_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(20L,
                List.of(centralCoreRoleInGisu(OTHER_GISU_ID)));
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }

        @Test
        @DisplayName("해당 기수의 중앙총괄(SUPER_ADMIN 아님)은 생성자가 아니면 거부")
        void 해당_기수_중앙총괄은_생성자_아니면_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(20L,
                List.of(centralCoreRoleInGisu(SCHEDULE_GISU_ID)));
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }
    }

    @Nested
    @DisplayName("FORCE_DELETE - 강제 삭제 권한")
    class forceDelete {

        @Test
        @DisplayName("SUPER_ADMIN system role이면 허용")
        void SUPER_ADMIN_system_role_허용() {
            givenSchedule();

            SubjectAttributes subject = superAdminSubject(20L);
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.FORCE_DELETE);

            assertThat(sut.evaluate(subject, permission)).isTrue();
        }

        @Test
        @DisplayName("일정 생성자 본인이라도 SUPER_ADMIN이 아니면 거부")
        void 생성자_본인이라도_SUPER_ADMIN_아니면_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(AUTHOR_MEMBER_ID, List.of());
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.FORCE_DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }

        @Test
        @DisplayName("다른 기수의 중앙총괄이면 거부")
        void 다른_기수_중앙총괄_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(20L,
                List.of(centralCoreRoleInGisu(OTHER_GISU_ID)));
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.FORCE_DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }

        @Test
        @DisplayName("해당 기수 중앙총괄(SUPER_ADMIN 아님)이면 거부")
        void 해당_기수_중앙총괄_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(20L,
                List.of(centralCoreRoleInGisu(SCHEDULE_GISU_ID)));
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.FORCE_DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }

        @Test
        @DisplayName("아무 역할도 없는 사용자는 거부")
        void 일반_사용자_거부() {
            givenSchedule();

            SubjectAttributes subject = subjectWith(20L, List.of());
            ResourcePermission permission = ResourcePermission.of(
                ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.FORCE_DELETE);

            assertThat(sut.evaluate(subject, permission)).isFalse();
        }
    }
}
