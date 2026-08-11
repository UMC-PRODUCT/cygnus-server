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
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByMemberIdCommand;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductDepartmentUseCase;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.LinkUmcProductMemberAccountCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberResult;
import com.umc.product.inhouse.application.port.in.command.dto.ResetUmcProductAccountPasswordResult;
import com.umc.product.inhouse.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductResponsibilitiesCommand;
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
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductChapter;
import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.exception.InhouseErrorCode;
import com.umc.product.member.application.port.in.command.ProvisionMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.ProvisionMemberCommand;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
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
    LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
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
    CredentialAuthenticationUseCase credentialAuthenticationUseCase;
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
    void 기존_로그인_계정을_인원에_LINKED로_연동한다() {
        UmcProductMember member = member(30L);
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(loadUmcProductMemberPort.getByIdWithLock(30L)).willReturn(member);
        given(getMemberUseCase.getById(200L)).willReturn(memberInfo(200L, "linked@example.com"));
        given(loadUmcProductMemberAccountPort.existsByMemberId(200L)).willReturn(false);
        given(saveUmcProductMemberAccountPort.save(any())).willAnswer(invocation -> {
            UmcProductMemberAccount account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 7L);
            return account;
        });

        Long accountId = sut.linkAccount(new LinkUmcProductMemberAccountCommand(1L, 30L, 200L));

        assertThat(accountId).isEqualTo(7L);
        verify(saveUmcProductMemberAccountPort).save(argThat(account -> !account.isProvisioned()));
    }

    @Test
    void 이미_다른_인원에_연동된_계정은_연동할_수_없다() {
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(loadUmcProductMemberPort.getByIdWithLock(30L)).willReturn(member(30L));
        given(getMemberUseCase.getById(200L)).willReturn(memberInfo(200L, "linked@example.com"));
        given(loadUmcProductMemberAccountPort.existsByMemberId(200L)).willReturn(true);

        assertThatThrownBy(() -> sut.linkAccount(new LinkUmcProductMemberAccountCommand(1L, 30L, 200L)))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    void 자동_발급_계정의_임시_비밀번호를_재발급한다() {
        UmcProductMemberAccount account = account(30L, 500L, UmcProductMemberAccountType.PROVISIONED);
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(loadUmcProductMemberAccountPort.findByMemberId(500L)).willReturn(Optional.of(account));
        given(getMemberUseCase.getById(500L)).willReturn(
            memberInfo(500L, "jeong@university.neordinary.com")
        );
        given(tempPasswordGenerator.generate()).willReturn("NewTemp1!bbbbbbb");

        ResetUmcProductAccountPasswordResult result = sut.resetAccountPassword(30L, 500L, 1L);

        assertThat(result.email()).isEqualTo("jeong@university.neordinary.com");
        assertThat(result.temporaryPassword()).isEqualTo("NewTemp1!bbbbbbb");
        verify(credentialAuthenticationUseCase).resetPasswordByMemberId(
            new ResetPasswordByMemberIdCommand(500L, "NewTemp1!bbbbbbb")
        );
    }

    @Test
    void LINKED_계정은_임시_비밀번호를_재발급할_수_없다() {
        UmcProductMemberAccount account = account(30L, 500L, UmcProductMemberAccountType.LINKED);
        given(umcProductAccessPolicy.canManageUmcProduct(1L)).willReturn(true);
        given(loadUmcProductMemberAccountPort.findByMemberId(500L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> sut.resetAccountPassword(30L, 500L, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_NOT_RESETTABLE);

        then(credentialAuthenticationUseCase).shouldHaveNoInteractions();
    }

    @Test
    void 연동된_계정은_자기_인원의_하는_일을_수정할_수_있다() {
        UmcProductMember member = member(30L);
        UmcProductChapterMembership membership = chapterMembership(50L, member);
        UmcProductDepartmentParticipant participant = departmentParticipant(60L, member);
        given(loadUmcProductMemberPort.getByIdWithLock(30L)).willReturn(member);
        given(umcProductAccessPolicy.canManageMemberProfile(100L, 30L)).willReturn(true);
        given(loadUmcProductChapterMembershipPort.getById(50L)).willReturn(membership);
        given(loadUmcProductDepartmentParticipantPort.getById(60L)).willReturn(participant);

        sut.updateResponsibilities(new UpdateUmcProductResponsibilitiesCommand(
            100L,
            30L,
            List.of(new UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility(
                50L, "API 개발", "서버 API 구현"
            )),
            List.of(new UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility(
                60L, "제품 개발", "스프린트 개발"
            ))
        ));

        assertThat(membership.getResponsibilityTitle()).isEqualTo("API 개발");
        assertThat(participant.getResponsibilityTitle()).isEqualTo("제품 개발");
        verify(saveUmcProductChapterMembershipPort).save(membership);
        verify(saveUmcProductDepartmentParticipantPort).save(participant);
    }

    @Test
    void 같은_소속을_한_요청에서_중복_수정할_수_없다() {
        UmcProductMember member = member(30L);
        given(loadUmcProductMemberPort.getByIdWithLock(30L)).willReturn(member);
        given(umcProductAccessPolicy.canManageMemberProfile(100L, 30L)).willReturn(true);
        UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility update =
            new UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility(50L, "API", null);

        assertThatThrownBy(() -> sut.updateResponsibilities(new UpdateUmcProductResponsibilitiesCommand(
            100L, 30L, List.of(update, update), List.of()
        )))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_RESPONSIBILITY_DUPLICATED);

        then(saveUmcProductChapterMembershipPort).shouldHaveNoInteractions();
    }

    @Test
    void 마지막_Department_참여가_다른_인원_소유면_어떤_하는_일도_수정하지_않는다() {
        UmcProductMember target = member(30L);
        UmcProductChapterMembership membership = chapterMembership(50L, target);
        UmcProductDepartmentParticipant otherParticipant = departmentParticipant(60L, member(31L));
        given(loadUmcProductMemberPort.getByIdWithLock(30L)).willReturn(target);
        given(umcProductAccessPolicy.canManageMemberProfile(100L, 30L)).willReturn(true);
        given(loadUmcProductChapterMembershipPort.getById(50L)).willReturn(membership);
        given(loadUmcProductDepartmentParticipantPort.getById(60L)).willReturn(otherParticipant);

        assertThatThrownBy(() -> sut.updateResponsibilities(new UpdateUmcProductResponsibilitiesCommand(
            100L,
            30L,
            List.of(new UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility(50L, "변경", null)),
            List.of(new UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility(60L, "변경", null))
        )))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_PARTICIPANT_NOT_FOUND);

        assertThat(membership.getResponsibilityTitle()).isEqualTo("기존 책임");
        then(saveUmcProductChapterMembershipPort).shouldHaveNoInteractions();
        then(saveUmcProductDepartmentParticipantPort).shouldHaveNoInteractions();
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

    private UmcProductMemberAccount account(
        Long umcProductMemberId,
        Long memberId,
        UmcProductMemberAccountType accountType
    ) {
        return UmcProductMemberAccount.create(member(umcProductMemberId), memberId, accountType);
    }

    private MemberInfo memberInfo(Long memberId, String email) {
        return new MemberInfo(
            memberId,
            "정의찬",
            "제옹",
            email,
            null,
            null,
            null,
            null,
            MemberStatus.ACTIVE,
            List.of()
        );
    }

    private UmcProductChapterMembership chapterMembership(Long id, UmcProductMember member) {
        UmcProductMemberActivityPeriod period = activityPeriod(member);
        UmcProductChapterMembership membership = UmcProductChapterMembership.create(
            period,
            UmcProductChapter.create("SERVER", "Server", null, 1, true),
            UmcProductPosition.SERVER_DEVELOPER,
            "기존 책임",
            "기존 설명",
            period.getStartDate(),
            period.getEndDate()
        );
        ReflectionTestUtils.setField(membership, "id", id);
        return membership;
    }

    private UmcProductDepartmentParticipant departmentParticipant(Long id, UmcProductMember member) {
        UmcProductMemberActivityPeriod period = activityPeriod(member);
        UmcProductDepartment department = UmcProductDepartment.create(
            "PLATFORM", "Platform", null, null, period.getStartDate(), period.getEndDate(), 1, true
        );
        UmcProductDepartmentParticipant participant = UmcProductDepartmentParticipant.create(
            department,
            period,
            UmcProductDepartmentRole.MEMBER,
            UmcProductPosition.SERVER_DEVELOPER,
            "기존 책임",
            "기존 설명",
            period.getStartDate(),
            period.getEndDate()
        );
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    private UmcProductMemberActivityPeriod activityPeriod(UmcProductMember member) {
        return UmcProductMemberActivityPeriod.create(
            member,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31)
        );
    }
}
