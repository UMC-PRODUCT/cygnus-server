package com.umc.product.organization.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 멤버 명령 서비스")
class UmcProductMemberCommandServiceTest {

    @Mock
    LoadUmcProductMemberPort loadUmcProductMemberPort;
    @Mock
    SaveUmcProductMemberPort saveUmcProductMemberPort;
    @Mock
    LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    @Mock
    SaveUmcProductMemberActivityPeriodPort saveUmcProductMemberActivityPeriodPort;
    @Mock
    LoadUmcProductPartPort loadUmcProductPartPort;
    @Mock
    LoadUmcProductPartMembershipPort loadUmcProductPartMembershipPort;
    @Mock
    SaveUmcProductPartMembershipPort saveUmcProductPartMembershipPort;
    @Mock
    LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    @Mock
    SaveUmcProductLeadershipPort saveUmcProductLeadershipPort;
    @Mock
    LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;
    @Mock
    SaveUmcProductSquadParticipantPort saveUmcProductSquadParticipantPort;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    UmcProductAccessPolicy umcProductAccessPolicy;

    @InjectMocks
    UmcProductMemberCommandService sut;

    @Test
    void 본인은_Leadership이_없어도_프로필을_수정할_수_있다() {
        UmcProductMember member = member(1L, 100L);
        given(loadUmcProductMemberPort.getByIdWithLock(1L)).willReturn(member);
        given(umcProductAccessPolicy.canManageMemberProfile(100L, 100L)).willReturn(true);
        given(getFileUseCase.existsById("product-profile")).willReturn(true);

        sut.updateProfile(UpdateUmcProductMemberProfileCommand.of(
            1L,
            100L,
            "새 소개",
            "product-profile"
        ));

        then(saveUmcProductMemberPort).should().save(member);
    }

    @Test
    void 관리_권한이_없으면_본인의_활동_기간도_추가할_수_없다() {
        CreateUmcProductMemberActivityPeriodCommand command =
            CreateUmcProductMemberActivityPeriodCommand.of(
                1L,
                100L,
                LocalDate.of(2026, 7, 1),
                null
            );
        given(umcProductAccessPolicy.canManageUmcProduct(100L)).willReturn(false);

        assertThatThrownBy(() -> sut.createActivityPeriod(command))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);

        then(loadUmcProductMemberPort).shouldHaveNoInteractions();
        then(saveUmcProductMemberActivityPeriodPort).shouldHaveNoInteractions();
    }

    @Test
    void 기존_활동_기간과_겹치거나_인접한_기간은_추가할_수_없다() {
        UmcProductMember member = member(1L, 100L);
        LocalDate startDate = LocalDate.of(2026, 7, 11);
        LocalDate endDate = LocalDate.of(2026, 8, 1);
        given(umcProductAccessPolicy.canManageUmcProduct(999L)).willReturn(true);
        given(loadUmcProductMemberPort.getByIdWithLock(1L)).willReturn(member);
        given(loadUmcProductMemberActivityPeriodPort.existsOverlappingOrAdjacent(
            1L,
            startDate,
            endDate,
            null
        )).willReturn(true);

        assertThatThrownBy(() -> sut.createActivityPeriod(
            CreateUmcProductMemberActivityPeriodCommand.of(1L, 999L, startDate, endDate)
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);

        then(saveUmcProductMemberActivityPeriodPort).should(never()).save(any());
    }

    @Test
    void 멤버_생성_시_서로_겹치는_활동_기간을_등록할_수_없다() {
        givenCreateMemberPrerequisites();
        CreateUmcProductMemberCommand command = createMemberCommand(List.of(
            period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)),
            period(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31))
        ));

        assertActivityPeriodConflict(command);
    }

    @Test
    void 멤버_생성_시_빈_날짜_없이_인접한_활동_기간을_등록할_수_없다() {
        givenCreateMemberPrerequisites();
        CreateUmcProductMemberCommand command = createMemberCommand(List.of(
            period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)),
            period(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31))
        ));

        assertActivityPeriodConflict(command);
    }

    @Test
    void 하위_활동을_범위_밖으로_내보내도록_멤버_활동_기간을_축소할_수_없다() {
        UmcProductMember member = member(1L, 100L);
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            member,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(activityPeriod, "id", 10L);
        UmcProductPartMembership membership = org.mockito.Mockito.mock(UmcProductPartMembership.class);
        given(membership.getMemberActivityPeriod()).willReturn(activityPeriod);
        given(membership.getStartDate()).willReturn(LocalDate.of(2026, 1, 15));
        given(membership.getEndDate()).willReturn(LocalDate.of(2026, 6, 30));
        given(umcProductAccessPolicy.canManageUmcProduct(999L)).willReturn(true);
        given(loadUmcProductMemberPort.getByIdWithLock(1L)).willReturn(member);
        given(loadUmcProductMemberActivityPeriodPort.getById(10L)).willReturn(activityPeriod);
        given(loadUmcProductPartMembershipPort.listByUmcProductMemberId(1L))
            .willReturn(List.of(membership));

        assertThatThrownBy(() -> sut.updateActivityPeriod(
            UpdateUmcProductMemberActivityPeriodCommand.of(
                1L,
                10L,
                999L,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 12, 31)
            )
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);

        then(saveUmcProductMemberActivityPeriodPort).should(never()).save(any());
    }

    private void assertActivityPeriodConflict(CreateUmcProductMemberCommand command) {
        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);

        then(saveUmcProductMemberPort).should(never()).save(any());
        then(saveUmcProductMemberActivityPeriodPort).shouldHaveNoInteractions();
    }

    private void givenCreateMemberPrerequisites() {
        given(umcProductAccessPolicy.canManageUmcProduct(999L)).willReturn(true);
        given(loadUmcProductMemberPort.existsByMemberId(100L)).willReturn(false);
    }

    private CreateUmcProductMemberCommand createMemberCommand(
        List<UmcProductActivityPeriodCommand> periods
    ) {
        return CreateUmcProductMemberCommand.of(999L, 100L, "소개", null, periods);
    }

    private UmcProductActivityPeriodCommand period(LocalDate startDate, LocalDate endDate) {
        return UmcProductActivityPeriodCommand.of(startDate, endDate);
    }

    private UmcProductMember member(Long id, Long memberId) {
        UmcProductMember member = UmcProductMember.create(memberId, "소개", null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
