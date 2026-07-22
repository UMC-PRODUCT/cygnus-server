package com.umc.product.schedule.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

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

    @Test
    @DisplayName("READ와 WRITE는 SUPER_ADMIN 또는 챌린저 활동 이력을 요구한다")
    void read_and_write_require_challenger_or_super_admin() {
        SubjectAttributes challenger = SubjectAttributes.builder()
            .memberId(20L)
            .gisuChallengerInfos(List.of(GisuChallengerInfo.builder().gisuId(1L).challengerId(1L).build()))
            .build();
        SubjectAttributes plainMember = subjectWith(20L, List.of());

        assertThat(sut.evaluate(challenger,
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.READ))).isTrue();
        assertThat(sut.evaluate(challenger,
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.WRITE))).isTrue();
        assertThat(sut.evaluate(plainMember,
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.READ))).isFalse();
    }

    @Test
    @DisplayName("EDIT는 resource ID와 존재하는 일정의 작성자 조건을 검증한다")
    void edit_requires_resource_and_author() {
        SubjectAttributes author = subjectWith(AUTHOR_MEMBER_ID, List.of());
        assertThat(sut.evaluate(author,
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.EDIT))).isFalse();

        givenSchedule();
        assertThat(sut.evaluate(author,
            ResourcePermission.of(ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.EDIT))).isTrue();
    }

    @Test
    @DisplayName("수정·강제 삭제 대상 일정이 없으면 not-found를 반환한다")
    void target_schedule_not_found() {
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.evaluate(subjectWith(AUTHOR_MEMBER_ID, List.of()),
            ResourcePermission.of(ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.EDIT)))
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND)
            );
        assertThatThrownBy(() -> sut.evaluate(superAdminSubject(AUTHOR_MEMBER_ID),
            ResourcePermission.of(ResourceType.SCHEDULE, SCHEDULE_ID, PermissionType.FORCE_DELETE)))
            .isInstanceOf(ScheduleDomainException.class);
    }

    @Test
    @DisplayName("강제 삭제에 resource ID가 없거나 permission을 지원하지 않으면 fail-closed 처리한다")
    void missing_resource_and_unsupported_permission_are_denied() {
        assertThat(sut.evaluate(superAdminSubject(AUTHOR_MEMBER_ID),
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.FORCE_DELETE))).isFalse();
        ResourcePermission unsupported = mock(ResourcePermission.class);
        given(unsupported.permission()).willReturn(PermissionType.APPROVE);
        assertThat(sut.evaluate(subjectWith(AUTHOR_MEMBER_ID, List.of()), unsupported)).isFalse();
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
