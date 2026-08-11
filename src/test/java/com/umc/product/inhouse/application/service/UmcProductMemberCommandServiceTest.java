package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductDepartmentUseCase;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberResult;
import com.umc.product.inhouse.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.exception.InhouseErrorCode;
import com.umc.product.member.application.port.in.command.ProvisionMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.ProvisionMemberCommand;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
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
    SaveUmcProductMemberAccountPort saveUmcProductMemberAccountPort;
    @Mock
    LoadUmcProductChapterPort loadUmcProductChapterPort;
    @Mock
    LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    @Mock
    SaveUmcProductChapterMembershipPort saveUmcProductChapterMembershipPort;
    @Mock
    LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    @Mock
    SaveUmcProductLeadershipPort saveUmcProductLeadershipPort;
    @Mock
    LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    @Mock
    SaveUmcProductDepartmentParticipantPort saveUmcProductDepartmentParticipantPort;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    ProvisionMemberUseCase provisionMemberUseCase;
    @Mock
    ManageUmcProductDepartmentUseCase manageUmcProductDepartmentUseCase;
    @Mock
    UmcProductTempPasswordGenerator tempPasswordGenerator;
    @Mock
    UmcProductAccessPolicy umcProductAccessPolicy;

    @InjectMocks
    UmcProductMemberCommandService sut;

    @Test
    void 본인은_Leadership이_없어도_프로필을_수정할_수_있다() {
        UmcProductMember member = member(1L);
        given(loadUmcProductMemberPort.getByIdWithLock(1L)).willReturn(member);
        given(umcProductAccessPolicy.canManageMemberProfile(100L, 1L)).willReturn(true);
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
    void 영어_닉네임으로_계정을_발급하고_인원에_연동한다() {
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(getMemberUseCase.existsByEmail("jeong@university.neordinary.com")).willReturn(false);
        given(tempPasswordGenerator.generate()).willReturn("TempPass1!aaaaaa");
        given(provisionMemberUseCase.provision(any())).willReturn(500L);
        given(saveUmcProductMemberPort.save(any())).willAnswer(invocation -> {
            UmcProductMember member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", 30L);
            return member;
        });
        given(saveUmcProductMemberActivityPeriodPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        RegisterUmcProductMemberResult result = sut.register(registerCommand("jeong"));

        assertThat(result.umcProductMemberId()).isEqualTo(30L);
        assertThat(result.memberId()).isEqualTo(500L);
        assertThat(result.email()).isEqualTo("jeong@university.neordinary.com");
        assertThat(result.temporaryPassword()).isEqualTo("TempPass1!aaaaaa");
        verify(provisionMemberUseCase).provision(new ProvisionMemberCommand(
            "정의찬", "제옹", "jeong@university.neordinary.com", null, "TempPass1!aaaaaa"
        ));
        verify(saveUmcProductMemberAccountPort).save(argThat(account ->
            account.getMemberId().equals(500L) && account.isProvisioned()
        ));
    }

    @Test
    void 잘못된_영어_닉네임이면_등록을_거부한다() {
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.register(registerCommand("Jeong!")))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ENGLISH_NICKNAME_INVALID);

        then(provisionMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    void 이미_사용_중인_발급_이메일이면_등록을_거부한다() {
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(getMemberUseCase.existsByEmail("jeong@university.neordinary.com")).willReturn(true);

        assertThatThrownBy(() -> sut.register(registerCommand("jeong")))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_EMAIL_ALREADY_EXISTS);

        then(provisionMemberUseCase).shouldHaveNoInteractions();
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
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);

        then(loadUmcProductMemberPort).shouldHaveNoInteractions();
        then(saveUmcProductMemberActivityPeriodPort).shouldHaveNoInteractions();
    }

    @Test
    void 기존_활동_기간과_겹치거나_인접한_기간은_추가할_수_없다() {
        UmcProductMember member = member(1L);
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
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);

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
        UmcProductMember member = member(1L);
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            member,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(activityPeriod, "id", 10L);
        UmcProductChapterMembership membership = org.mockito.Mockito.mock(UmcProductChapterMembership.class);
        given(membership.getMemberActivityPeriod()).willReturn(activityPeriod);
        given(membership.getStartDate()).willReturn(LocalDate.of(2026, 1, 15));
        given(membership.getEndDate()).willReturn(LocalDate.of(2026, 6, 30));
        given(umcProductAccessPolicy.canManageUmcProduct(999L)).willReturn(true);
        given(loadUmcProductMemberPort.getByIdWithLock(1L)).willReturn(member);
        given(loadUmcProductMemberActivityPeriodPort.getById(10L)).willReturn(activityPeriod);
        given(loadUmcProductChapterMembershipPort.listByUmcProductMemberId(1L))
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
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);

        then(saveUmcProductMemberActivityPeriodPort).should(never()).save(any());
    }

    private void assertActivityPeriodConflict(CreateUmcProductMemberCommand command) {
        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);

        then(saveUmcProductMemberPort).should(never()).save(any());
        then(saveUmcProductMemberActivityPeriodPort).shouldHaveNoInteractions();
    }

    private void givenCreateMemberPrerequisites() {
        given(umcProductAccessPolicy.canManageUmcProduct(999L)).willReturn(true);
    }

    private CreateUmcProductMemberCommand createMemberCommand(
        List<UmcProductActivityPeriodCommand> periods
    ) {
        return CreateUmcProductMemberCommand.of(
            999L,
            "홍길동",
            "길동",
            null,
            "소개",
            null,
            periods
        );
    }

    private UmcProductActivityPeriodCommand period(LocalDate startDate, LocalDate endDate) {
        return UmcProductActivityPeriodCommand.of(startDate, endDate);
    }

    private RegisterUmcProductMemberCommand registerCommand(String englishNickname) {
        return new RegisterUmcProductMemberCommand(
            1L,
            "정의찬",
            "제옹",
            englishNickname,
            null,
            "소개",
            null,
            List.of(period(LocalDate.of(2026, 1, 1), null)),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private UmcProductMember member(Long id) {
        UmcProductMember member = UmcProductMember.create("홍길동", "길동", null, "소개", null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
