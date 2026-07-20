package com.umc.product.organization.application.service;

import static com.umc.product.support.fixture.OrganizationUnitFixture.기수;
import static com.umc.product.support.fixture.OrganizationUnitFixture.리더십;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드_참여;
import static com.umc.product.support.fixture.OrganizationUnitFixture.챕터_소속;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_멤버;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_챕터;
import static com.umc.product.support.fixture.OrganizationUnitFixture.활동_기간;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.GetWeeklyCurriculumUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateStudyGroupScheduleCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterCommand;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;
import com.umc.product.organization.application.port.out.command.SaveStudyGroupSchedulePort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.application.port.service.query.GisuQueryService;
import com.umc.product.organization.application.port.service.query.SchoolAccessContextService;
import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.schedule.application.port.in.query.GetScheduleUseCase;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleBaseInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@DisplayName("Organization 잔여 application service 경로")
class OrganizationRemainingServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("UMC PRODUCT 멤버 조회 정렬")
    class MemberQueryComparator {
        @Mock
        LoadUmcProductMemberPort memberPort;
        @Mock
        LoadUmcProductMemberActivityPeriodPort periodPort;
        @Mock
        LoadUmcProductChapterMembershipPort membershipPort;
        @Mock
        LoadUmcProductLeadershipPort leadershipPort;
        @Mock
        LoadUmcProductSquadParticipantPort participantPort;
        @Mock
        LoadUmcProductSquadPort squadPort;
        @Mock
        GetMemberUseCase memberUseCase;
        @Mock
        GetFileUseCase fileUseCase;

        @Test
        @DisplayName("모든 활동 이력 타입의 시작일과 ID를 추출해 최신순으로 비교한다")
        void 모든_이력_타입을_비교한다() {
            var service = service();
            var member = 프로덕트_멤버(1L, 10L, null);
            var period1 = 활동_기간(2L, member, START, END);
            var period2 = 활동_기간(3L, member, START.plusDays(1), END);
            var period3 = 활동_기간(3L, member, START, END);
            var membership = 챕터_소속(4L, period1, 프로덕트_챕터(5L), START, END);
            var leadership = 리더십(6L, period1, START, END);
            var participant = 스쿼드_참여(7L, 스쿼드(8L, START, END), period1, START, END);

            @SuppressWarnings("unchecked")
            Comparator<Object> comparator = ReflectionTestUtils.invokeMethod(service, "historyComparator");

            assertThat(comparator.compare(period1, period2)).isPositive();
            assertThat(comparator.compare(period1, period3)).isPositive();
            assertThat((LocalDate) ReflectionTestUtils.invokeMethod(service, "startDateOf", membership)).isEqualTo(START);
            assertThat((LocalDate) ReflectionTestUtils.invokeMethod(service, "startDateOf", leadership)).isEqualTo(START);
            assertThat((LocalDate) ReflectionTestUtils.invokeMethod(service, "startDateOf", participant)).isEqualTo(START);
            assertThat((Long) ReflectionTestUtils.invokeMethod(service, "idOf", membership)).isEqualTo(4L);
            assertThat((Long) ReflectionTestUtils.invokeMethod(service, "idOf", leadership)).isEqualTo(6L);
            assertThat((Long) ReflectionTestUtils.invokeMethod(service, "idOf", participant)).isEqualTo(7L);
        }

        private UmcProductMemberQueryService service() {
            return new UmcProductMemberQueryService(
                memberPort, periodPort, membershipPort, leadershipPort,
                participantPort, squadPort, memberUseCase, fileUseCase
            );
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("UMC PRODUCT 챕터 명령")
    class ChapterCommand {
        @Mock
        LoadUmcProductChapterPort loadPort;
        @Mock
        SaveUmcProductChapterPort savePort;
        @Mock
        LoadUmcProductChapterMembershipPort membershipPort;
        @Mock
        UmcProductAccessPolicy accessPolicy;

        @Test
        @DisplayName("생성·수정·삭제 수명주기와 중복·소속·권한 예외를 검증한다")
        void 챕터_수명주기와_예외를_검증한다() {
            var service = new UmcProductChapterCommandService(loadPort, savePort, membershipPort, accessPolicy);
            var chapter = 프로덕트_챕터(1L);
            given(accessPolicy.canManageUmcProduct(10L)).willReturn(true);
            given(savePort.save(any())).willReturn(chapter);
            given(loadPort.getByIdWithLock(1L)).willReturn(chapter);

            assertThat(service.create(CreateUmcProductChapterCommand.of(
                10L, "NEW", "New", "설명", 1, true
            ))).isEqualTo(1L);
            service.update(UpdateUmcProductChapterCommand.of(
                1L, 10L, "UPDATED", "Updated", "변경", 2, false
            ));
            service.delete(1L, 10L);

            given(loadPort.existsByCode("DUP", null)).willReturn(true);
            assertThatThrownBy(() -> service.create(CreateUmcProductChapterCommand.of(
                10L, " DUP ", "중복", null, 1, true
            ))).isInstanceOf(OrganizationDomainException.class);
            given(membershipPort.existsByChapterId(1L)).willReturn(true);
            assertThatThrownBy(() -> service.delete(1L, 10L)).isInstanceOf(OrganizationDomainException.class);
            given(accessPolicy.canManageUmcProduct(99L)).willReturn(false);
            assertThatThrownBy(() -> service.create(CreateUmcProductChapterCommand.of(
                99L, "DENIED", "거부", null, 1, true
            ))).isInstanceOf(OrganizationDomainException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("기수 조회")
    class GisuQuery {
        @Mock
        LoadGisuPort loadPort;

        @Test
        @DisplayName("ID·세대 batch·활성·날짜 조회의 빈값과 not-found를 처리한다")
        void 기수_조회_경계를_처리한다() {
            var service = new GisuQueryService(loadPort);
            var gisu = 기수(1L, 9L, true);
            given(loadPort.listByIds(Set.of(1L))).willReturn(List.of(gisu));
            given(loadPort.listByGenerations(Set.of(9L))).willReturn(List.of(gisu));
            given(loadPort.getActiveGisu()).willReturn(gisu);
            given(loadPort.findActiveGisu()).willReturn(Optional.of(gisu));
            given(loadPort.findGisuByDate(Instant.EPOCH)).willReturn(Optional.of(gisu));
            given(loadPort.findGisuByDate(Instant.MAX)).willReturn(Optional.empty());

            assertThat(service.getByIds(Set.of(1L))).hasSize(1);
            assertThat(service.batchGetByIds(null)).isEmpty();
            assertThat(service.batchGetByIds(Arrays.asList(null, null))).isEmpty();
            assertThat(service.batchGetByIds(List.of(1L, 1L))).hasSize(1);
            assertThat(service.batchGetByGenerations(null)).isEmpty();
            assertThat(service.batchGetByGenerations(List.of(9L, 9L))).hasSize(1);
            assertThat(service.getActiveGisuId()).isEqualTo(1L);
            assertThat(service.findActiveGisu()).isPresent();
            assertThat(service.getGisuByDate(Instant.EPOCH).gisuId()).isEqualTo(1L);
            assertThatThrownBy(() -> service.getGisuByDate(Instant.MAX))
                .isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> service.batchGetByIds(List.of(1L, 2L)))
                .isInstanceOf(OrganizationDomainException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("학교 접근 컨텍스트")
    class SchoolAccess {
        @Mock
        GetMemberUseCase memberUseCase;
        @Mock
        GisuQueryService gisuUseCase;
        @Mock
        GetChallengerRoleUseCase roleUseCase;

        @Test
        @DisplayName("학교 운영진이 아니면 거부하고 회장단과 파트장의 조회 범위를 구분한다")
        void 학교_역할별_범위를_구분한다() {
            var service = new SchoolAccessContextService(memberUseCase, gisuUseCase, roleUseCase);
            given(memberUseCase.getById(1L)).willReturn(com.umc.product.member.application.port.in.query.dto.MemberInfo.builder()
                .id(1L).schoolId(100L).build());
            given(gisuUseCase.getActiveGisuId()).willReturn(9L);

            assertThatThrownBy(() -> service.getContext(1L)).isInstanceOf(OrganizationDomainException.class);

            given(roleUseCase.isSchoolAdminInGisu(1L, 9L, 100L)).willReturn(true);
            given(roleUseCase.isSchoolCoreInGisu(1L, 9L, 100L)).willReturn(true);
            assertThat(service.getContext(1L).part()).isNull();

            given(roleUseCase.isSchoolCoreInGisu(1L, 9L, 100L)).willReturn(false);
            given(roleUseCase.getAllResponsiblePartByMemberIdAndGisuId(1L, 9L))
                .willReturn(Set.of(ChallengerPart.SPRINGBOOT));
            assertThat(service.getContext(1L).part()).isEqualTo(ChallengerPart.SPRINGBOOT);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("스터디 일정")
    class StudySchedule {
        @Mock
        SaveStudyGroupSchedulePort savePort;
        @Mock
        GetStudyGroupUseCase studyGroupUseCase;
        @Mock
        GetScheduleUseCase scheduleUseCase;
        @Mock
        GetWeeklyCurriculumUseCase weeklyCurriculumUseCase;

        @Test
        @DisplayName("스터디·출석 정책·커리큘럼을 검증한 뒤 일정을 저장한다")
        void 일정_생성과_예외를_검증한다() {
            var service = new StudyGroupScheduleService(
                savePort, studyGroupUseCase, scheduleUseCase, weeklyCurriculumUseCase
            );
            var command = CreateStudyGroupScheduleCommand.builder()
                .studyGroupId(1L).scheduleId(2L).weeklyCurriculumId(3L).build();

            given(studyGroupUseCase.findById(1L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(OrganizationDomainException.class);

            given(studyGroupUseCase.findById(1L)).willReturn(Optional.of(
                StudyGroupInfo.create(1L, "스터디", 9L, ChallengerPart.SPRINGBOOT, null, List.of(), List.of())
            ));
            given(scheduleUseCase.getScheduleBaseInfo(2L)).willReturn(scheduleInfo(false));
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(OrganizationDomainException.class);

            given(scheduleUseCase.getScheduleBaseInfo(2L)).willReturn(scheduleInfo(true));
            var saved = command.toEntity();
            ReflectionTestUtils.setField(saved, "id", 10L);
            given(savePort.save(any(StudyGroupSchedule.class))).willReturn(saved);

            assertThat(service.create(command)).isEqualTo(10L);
        }

        private ScheduleBaseInfo scheduleInfo(boolean attendanceChecked) {
            return new ScheduleBaseInfo(
                2L, "일정", null, Set.of(), 1L, Instant.EPOCH, Instant.EPOCH,
                true, null, attendanceChecked, null
            );
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("간단 조회와 접근 정책")
    class SimpleQueries {
        @Mock
        LoadUmcProductChapterPort chapterPort;
        @Mock
        LoadUmcProductSquadPort squadPort;
        @Mock
        GetChallengerRoleUseCase roleUseCase;
        @Mock
        LoadUmcProductLeadershipPort leadershipPort;
        @Mock(answer = Answers.RETURNS_DEEP_STUBS)
        UmcProductDateProvider dateProvider;

        @Test
        @DisplayName("챕터·스쿼드 목록을 조회 모델로 변환한다")
        void 챕터와_스쿼드_목록을_변환한다() {
            given(chapterPort.listAll(true)).willReturn(List.of(프로덕트_챕터(1L)));
            given(squadPort.listAll(true, START)).willReturn(List.of(스쿼드(1L, START, END)));

            assertThat(new UmcProductChapterQueryService(chapterPort).list(true)).hasSize(1);
            assertThat(new UmcProductSquadQueryService(squadPort).list(true, START)).hasSize(1);
        }

        @Test
        @DisplayName("접근 정책은 null 요청자와 타인 프로필 요청을 관리 권한으로 평가한다")
        void 접근_정책의_null과_타인_요청을_평가한다() {
            var policy = new UmcProductAccessPolicy(roleUseCase, leadershipPort, dateProvider);

            assertThat(policy.canManageUmcProduct(null)).isFalse();
            assertThat(policy.canManageMemberProfile(1L, 2L)).isFalse();
        }
    }
}
