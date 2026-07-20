package com.umc.product.organization.application.service;

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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductChapterMembership;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 멤버 명령 서비스 엣지 케이스")
class UmcProductMemberCommandServiceEdgeCaseTest {

    private static final Long REQUESTER_ID = 999L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Mock
    LoadUmcProductMemberPort loadMemberPort;
    @Mock
    SaveUmcProductMemberPort saveMemberPort;
    @Mock
    LoadUmcProductMemberActivityPeriodPort loadPeriodPort;
    @Mock
    SaveUmcProductMemberActivityPeriodPort savePeriodPort;
    @Mock
    LoadUmcProductChapterPort loadChapterPort;
    @Mock
    LoadUmcProductChapterMembershipPort loadMembershipPort;
    @Mock
    SaveUmcProductChapterMembershipPort saveMembershipPort;
    @Mock
    LoadUmcProductLeadershipPort loadLeadershipPort;
    @Mock
    SaveUmcProductLeadershipPort saveLeadershipPort;
    @Mock
    LoadUmcProductSquadParticipantPort loadParticipantPort;
    @Mock
    SaveUmcProductSquadParticipantPort saveParticipantPort;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    UmcProductAccessPolicy accessPolicy;

    @InjectMocks
    UmcProductMemberCommandService sut;

    @BeforeEach
    void 관리_권한을_허용한다() {
        lenient().when(accessPolicy.canManageUmcProduct(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    @DisplayName("멤버 생성 시 정렬되지 않은 비인접 활동 기간을 모두 저장한다")
    void 멤버와_초기_활동_기간을_생성한다() {
        UmcProductMember saved = member();
        given(saveMemberPort.save(any())).willReturn(saved);
        CreateUmcProductMemberCommand command = CreateUmcProductMemberCommand.of(
            REQUESTER_ID,
            100L,
            "소개",
            " ",
            List.of(
                period(LocalDate.of(2026, 7, 2), END),
                period(START, LocalDate.of(2026, 6, 30))
            )
        );

        Long result = sut.create(command);

        assertThat(result).isEqualTo(1L);
        then(getFileUseCase).shouldHaveNoInteractions();
        then(savePeriodPort).should(times(2)).save(any());
    }

    @Test
    @DisplayName("이미 UMC PRODUCT에 등록된 회원은 중복 생성할 수 없다")
    void 중복_멤버_생성을_거부한다() {
        given(loadMemberPort.existsByMemberId(100L)).willReturn(true);

        assertError(() -> sut.create(createMember(List.of(period(START, END)))),
            OrganizationErrorCode.UMC_PRODUCT_MEMBER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("초기 활동 기간은 null이거나 빈 목록일 수 없다")
    void 초기_활동_기간을_필수로_검증한다() {
        assertError(() -> sut.create(createMember(null)),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
        assertError(() -> sut.create(createMember(List.of())),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
    }

    @Test
    @DisplayName("초기 활동 기간의 시작일은 필수이고 종료일은 시작일보다 빠를 수 없다")
    void 초기_활동_기간의_날짜를_검증한다() {
        assertError(() -> sut.create(createMember(List.of(period(null, END)))),
            OrganizationErrorCode.UMC_PRODUCT_START_DATE_REQUIRED);
        assertError(() -> sut.create(createMember(List.of(period(END, START)))),
            OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
    }

    @Test
    @DisplayName("무기한 활동 기간 뒤에는 다른 초기 활동 기간을 둘 수 없다")
    void 무기한_기간_뒤의_기간을_거부한다() {
        assertError(() -> sut.create(createMember(List.of(
            period(START, null),
            period(LocalDate.of(2027, 1, 1), null)
        ))), OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);
    }

    @Test
    @DisplayName("LocalDate 최대 종료일은 overflow 없이 중복 기간으로 처리한다")
    void 최대_종료일을_안전하게_검증한다() {
        assertError(() -> sut.create(createMember(List.of(
            period(START, LocalDate.MAX),
            period(LocalDate.MAX, LocalDate.MAX)
        ))), OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);
    }

    @Test
    @DisplayName("존재하지 않는 프로필 이미지로 멤버를 생성할 수 없다")
    void 프로필_이미지_누락을_거부한다() {
        given(getFileUseCase.existsById("missing-image")).willReturn(false);
        CreateUmcProductMemberCommand command = CreateUmcProductMemberCommand.of(
            REQUESTER_ID, 100L, "소개", "missing-image", List.of(period(START, END)));

        assertThatThrownBy(() -> sut.create(command)).isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("다른 회원의 프로필 수정 권한이 없으면 변경을 거부한다")
    void 프로필_수정_권한을_검증한다() {
        UmcProductMember member = member();
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member);
        given(accessPolicy.canManageMemberProfile(REQUESTER_ID, 100L)).willReturn(false);

        assertError(() -> sut.updateProfile(
            UpdateUmcProductMemberProfileCommand.of(1L, REQUESTER_ID, "변경", null)),
            OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 프로필 이미지로 프로필을 수정할 수 없다")
    void 프로필_수정_이미지를_검증한다() {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(accessPolicy.canManageMemberProfile(REQUESTER_ID, 100L)).willReturn(true);
        given(getFileUseCase.existsById("missing-image")).willReturn(false);

        assertThatThrownBy(() -> sut.updateProfile(
            UpdateUmcProductMemberProfileCommand.of(1L, REQUESTER_ID, "변경", "missing-image")))
            .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("멤버 삭제는 모든 하위 이력을 먼저 삭제한다")
    void 멤버와_하위_이력을_삭제한다() {
        UmcProductMember member = member();
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member);

        sut.delete(1L, REQUESTER_ID);

        then(saveParticipantPort).should().deleteAllByUmcProductMemberId(1L);
        then(saveMembershipPort).should().deleteAllByUmcProductMemberId(1L);
        then(saveLeadershipPort).should().deleteAllByUmcProductMemberId(1L);
        then(savePeriodPort).should().deleteAllByUmcProductMemberId(1L);
        then(saveMemberPort).should().delete(member);
    }

    @Test
    @DisplayName("겹치지 않는 활동 기간을 추가하고 생성 ID를 반환한다")
    void 활동_기간을_생성한다() {
        UmcProductMemberActivityPeriod saved = activityPeriod();
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(savePeriodPort.save(any())).willReturn(saved);

        Long result = sut.createActivityPeriod(
            CreateUmcProductMemberActivityPeriodCommand.of(1L, REQUESTER_ID, START, null));

        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("다른 멤버 소유의 활동 기간은 수정할 수 없다")
    void 다른_멤버의_활동_기간_수정을_거부한다() {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.getById(10L)).willReturn(activityPeriod(anotherMember()));

        assertError(() -> sut.updateActivityPeriod(updatePeriod(START, END)),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND);
    }

    @Test
    @DisplayName("하위 활동이 모두 새 범위에 포함되면 활동 기간을 수정한다")
    void 활동_기간을_수정한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();
        UmcProductChapterMembership membership = 챕터_소속(
            20L, activityPeriod, chapter(), START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.getById(10L)).willReturn(activityPeriod);
        given(loadMembershipPort.listByUmcProductMemberId(1L)).willReturn(List.of(membership));
        given(loadLeadershipPort.listByUmcProductMemberId(1L)).willReturn(List.of());
        given(loadParticipantPort.listByUmcProductMemberId(1L)).willReturn(List.of());

        sut.updateActivityPeriod(updatePeriod(START, null));

        assertThat(activityPeriod.getEndDate()).isNull();
        then(savePeriodPort).should().save(activityPeriod);
    }

    @Test
    @DisplayName("리더십이 새 활동 기간 범위를 벗어나면 축소를 거부한다")
    void 범위_밖_리더십을_검증한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();
        UmcProductLeadership leadership = 리더십(30L, activityPeriod, START, END);
        givenUpdatePeriod(activityPeriod);
        given(loadMembershipPort.listByUmcProductMemberId(1L)).willReturn(List.of());
        given(loadLeadershipPort.listByUmcProductMemberId(1L)).willReturn(List.of(leadership));

        assertError(() -> sut.updateActivityPeriod(updatePeriod(START.plusDays(1), END)),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("스쿼드 참여가 새 활동 기간 범위를 벗어나면 축소를 거부한다")
    void 범위_밖_스쿼드_참여를_검증한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();
        UmcProductSquadParticipant participant = 스쿼드_참여(
            40L, squad(), activityPeriod, START, END);
        givenUpdatePeriod(activityPeriod);
        given(loadMembershipPort.listByUmcProductMemberId(1L)).willReturn(List.of());
        given(loadLeadershipPort.listByUmcProductMemberId(1L)).willReturn(List.of());
        given(loadParticipantPort.listByUmcProductMemberId(1L)).willReturn(List.of(participant));

        assertError(() -> sut.updateActivityPeriod(updatePeriod(START.plusDays(1), END)),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("연관 이력이 없는 활동 기간은 삭제한다")
    void 활동_기간을_삭제한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.getById(10L)).willReturn(activityPeriod);

        sut.deleteActivityPeriod(1L, 10L, REQUESTER_ID);

        then(savePeriodPort).should().delete(activityPeriod);
    }

    @Test
    @DisplayName("챕터·리더십·스쿼드 연관 중 하나라도 있으면 활동 기간을 삭제할 수 없다")
    void 연관_활동_기간_삭제를_거부한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.getById(10L)).willReturn(activityPeriod);
        given(loadMembershipPort.existsByMemberActivityPeriodId(10L)).willReturn(false);
        given(loadLeadershipPort.existsByMemberActivityPeriodId(10L)).willReturn(false);
        given(loadParticipantPort.existsByMemberActivityPeriodId(10L)).willReturn(true);

        assertError(() -> sut.deleteActivityPeriod(1L, 10L, REQUESTER_ID),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_HAS_ASSOCIATIONS);
    }

    @Test
    @DisplayName("활동 기간 안에 겹치지 않는 챕터 소속을 생성한다")
    void 챕터_소속을_생성한다() {
        UmcProductMemberActivityPeriod period = activityPeriod();
        UmcProductChapterMembership saved = 챕터_소속(20L, period, chapter(), START, END);
        givenContainingPeriod(period);
        given(loadChapterPort.getById(2L)).willReturn(chapter());
        given(saveMembershipPort.save(any())).willReturn(saved);

        Long result = sut.createChapterMembership(createMembership());

        assertThat(result).isEqualTo(20L);
    }

    @Test
    @DisplayName("챕터 소속 기간을 포함하는 활동 기간이 없으면 생성을 거부한다")
    void 챕터_소속의_범위를_검증한다() {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.empty());

        assertError(() -> sut.createChapterMembership(createMembership()),
            OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("같은 챕터의 기간이 겹치면 소속 생성을 거부한다")
    void 중복_챕터_소속을_거부한다() {
        givenContainingPeriod(activityPeriod());
        given(loadChapterPort.getById(2L)).willReturn(chapter());
        given(loadMembershipPort.existsOverlappingChapterMembership(1L, 2L, START, END, null))
            .willReturn(true);

        assertError(() -> sut.createChapterMembership(createMembership()),
            OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED);
    }

    @Test
    @DisplayName("본인 소속을 새 챕터와 활동 기간으로 수정한다")
    void 챕터_소속을_수정한다() {
        UmcProductMemberActivityPeriod period = activityPeriod();
        UmcProductChapterMembership membership = 챕터_소속(20L, period, chapter(), START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadMembershipPort.getById(20L)).willReturn(membership);
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.of(period));
        given(loadChapterPort.getById(2L)).willReturn(chapter());

        sut.updateChapterMembership(updateMembership());

        then(saveMembershipPort).should().save(membership);
        then(loadMembershipPort).should().existsOverlappingChapterMembership(1L, 2L, START, END, 20L);
    }

    @Test
    @DisplayName("다른 멤버의 챕터 소속은 수정하거나 삭제할 수 없다")
    void 다른_멤버의_챕터_소속을_거부한다() {
        UmcProductChapterMembership membership = 챕터_소속(
            20L, activityPeriod(anotherMember()), chapter(), START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadMembershipPort.getById(20L)).willReturn(membership);

        assertError(() -> sut.updateChapterMembership(updateMembership()),
            OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND);
        assertError(() -> sut.deleteChapterMembership(1L, 20L, REQUESTER_ID),
            OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 챕터 소속을 삭제한다")
    void 챕터_소속을_삭제한다() {
        UmcProductChapterMembership membership = 챕터_소속(20L, activityPeriod(), chapter(), START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadMembershipPort.getById(20L)).willReturn(membership);

        sut.deleteChapterMembership(1L, 20L, REQUESTER_ID);

        then(saveMembershipPort).should().delete(membership);
    }

    @Test
    @DisplayName("활동 기간 안에 겹치지 않는 리더십을 생성한다")
    void 리더십을_생성한다() {
        UmcProductMemberActivityPeriod period = activityPeriod();
        UmcProductLeadership saved = 리더십(30L, period, START, END);
        givenContainingPeriod(period);
        given(saveLeadershipPort.save(any())).willReturn(saved);

        assertThat(sut.createLeadership(createLeadership())).isEqualTo(30L);
    }

    @Test
    @DisplayName("동일 역할 또는 동일 멤버의 리더십 기간 중복을 거부한다")
    void 중복_리더십을_거부한다() {
        givenContainingPeriod(activityPeriod());
        given(loadLeadershipPort.existsOverlappingRole(
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START, END, null)).willReturn(false);
        given(loadLeadershipPort.existsOverlappingMember(1L, START, END, null)).willReturn(true);

        assertError(() -> sut.createLeadership(createLeadership()),
            OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED);
    }

    @Test
    @DisplayName("본인 리더십을 수정하고 기존 ID를 중복 검사에서 제외한다")
    void 리더십을_수정한다() {
        UmcProductMemberActivityPeriod period = activityPeriod();
        UmcProductLeadership leadership = 리더십(30L, period, START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadLeadershipPort.getById(30L)).willReturn(leadership);
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.of(period));

        sut.updateLeadership(updateLeadership());

        then(saveLeadershipPort).should().save(leadership);
        then(loadLeadershipPort).should().existsOverlappingRole(
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START, END, 30L);
    }

    @Test
    @DisplayName("다른 멤버의 리더십은 수정하거나 삭제할 수 없다")
    void 다른_멤버의_리더십을_거부한다() {
        UmcProductLeadership leadership = 리더십(30L, activityPeriod(anotherMember()), START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadLeadershipPort.getById(30L)).willReturn(leadership);

        assertError(() -> sut.updateLeadership(updateLeadership()),
            OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_NOT_FOUND);
        assertError(() -> sut.deleteLeadership(1L, 30L, REQUESTER_ID),
            OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 리더십을 삭제한다")
    void 리더십을_삭제한다() {
        UmcProductLeadership leadership = 리더십(30L, activityPeriod(), START, END);
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadLeadershipPort.getById(30L)).willReturn(leadership);

        sut.deleteLeadership(1L, 30L, REQUESTER_ID);

        then(saveLeadershipPort).should().delete(leadership);
    }

    private void givenContainingPeriod(UmcProductMemberActivityPeriod period) {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.findContaining(1L, START, END)).willReturn(Optional.of(period));
    }

    private void givenUpdatePeriod(UmcProductMemberActivityPeriod period) {
        given(loadMemberPort.getByIdWithLock(1L)).willReturn(member());
        given(loadPeriodPort.getById(10L)).willReturn(period);
    }

    private void assertError(Runnable action, OrganizationErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(errorCode);
    }

    private CreateUmcProductMemberCommand createMember(List<UmcProductActivityPeriodCommand> periods) {
        return CreateUmcProductMemberCommand.of(REQUESTER_ID, 100L, "소개", null, periods);
    }

    private UmcProductActivityPeriodCommand period(LocalDate start, LocalDate end) {
        return UmcProductActivityPeriodCommand.of(start, end);
    }

    private UpdateUmcProductMemberActivityPeriodCommand updatePeriod(LocalDate start, LocalDate end) {
        return UpdateUmcProductMemberActivityPeriodCommand.of(1L, 10L, REQUESTER_ID, start, end);
    }

    private CreateUmcProductChapterMembershipCommand createMembership() {
        return CreateUmcProductChapterMembershipCommand.of(
            1L, REQUESTER_ID, 2L, UmcProductPosition.SERVER_DEVELOPER,
            "Backend", "API 개발", START, END);
    }

    private UpdateUmcProductChapterMembershipCommand updateMembership() {
        return UpdateUmcProductChapterMembershipCommand.of(
            1L, 20L, REQUESTER_ID, 2L, UmcProductPosition.SERVER_DEVELOPER,
            "Backend", "API 개발", START, END);
    }

    private CreateUmcProductLeadershipCommand createLeadership() {
        return CreateUmcProductLeadershipCommand.of(
            1L, REQUESTER_ID, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START, END);
    }

    private UpdateUmcProductLeadershipCommand updateLeadership() {
        return UpdateUmcProductLeadershipCommand.of(
            1L, 30L, REQUESTER_ID, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START, END);
    }

    private UmcProductMember member() {
        return 프로덕트_멤버(1L, 100L, null);
    }

    private UmcProductMember anotherMember() {
        return 프로덕트_멤버(9L, 900L, null);
    }

    private UmcProductMemberActivityPeriod activityPeriod() {
        return activityPeriod(member());
    }

    private UmcProductMemberActivityPeriod activityPeriod(UmcProductMember member) {
        return 활동_기간(10L, member, START, END);
    }

    private UmcProductChapter chapter() {
        return 프로덕트_챕터(2L);
    }

    private UmcProductSquad squad() {
        return 스쿼드(3L, START, END);
    }
}
