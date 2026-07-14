package com.umc.product.member.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 이메일 기반 회원가입 서비스. ADR-017 흐름.
 * <p>
 * loginId 는 받지 않으며, 이메일은 emailVerificationToken 에서 추출되어 이미 Command 에 포함되어 있다.
 */
@Service
@RequiredArgsConstructor
public class EmailMemberRegisterService implements RegisterEmailMemberUseCase {

    private final SaveMemberPort saveMemberPort;

    private final MemberRegistrationValidator registrationValidator;

    private final CredentialAuthenticationUseCase credentialAuthenticationUseCase;
    private final ManageTermAgreementUseCase manageTermAgreementUseCase;
    private final GetSchoolUseCase getSchoolUseCase;

    private final RecordAuditLogUseCase recordAuditLogUseCase;

    @Override
    @Transactional
    public Long register(EmailRegisterMemberCommand command) {
        return registerInternal(command);
    }

    /**
     * atomic batch 등록. 한 트랜잭션 안에서 N 건을 순차 처리하므로 동일 schoolId 의 검증 SELECT 는
     * 1 차 캐시에 의해 1 회로 묶이고, 트랜잭션 commit 도 N → 1 회로 감소한다. 도메인 검증과 자격증명
     * 등록은 단건과 동일하게 매 command 별로 수행된다. 한 건 실패 시 전체 롤백.
     */
    @Override
    @Transactional
    public List<Long> batchRegister(List<EmailRegisterMemberCommand> commands) {
        if (commands.isEmpty()) {
            return List.of();
        }
        List<Long> registeredIds = new ArrayList<>(commands.size());
        for (EmailRegisterMemberCommand command : commands) {
            registeredIds.add(registerInternal(command));
        }
        return registeredIds;
    }

    private Long registerInternal(EmailRegisterMemberCommand command) {
        registrationValidator.validateSchoolExists(command.schoolId());
        registrationValidator.validateMandatoryTermsAgreed(command.termConsents());

        Member created = saveMemberPort.save(command.toEntity());

        credentialAuthenticationUseCase.registerCredentialByEmail(
            RegisterCredentialByEmailCommand.of(created.getId(), command.rawPassword())
        );

        command.termConsents().forEach(termConsent ->
            manageTermAgreementUseCase.createTermConsent(termConsent.toCommand(created.getId()))
        );

        SchoolDetailInfo school = getSchoolUseCase.getSchoolDetail(command.schoolId());
        MemberAuditEventFactory.MemberSnapshot memberSnapshot =
            MemberAuditEventFactory.snapshot(created, school.schoolName());
        recordAuditLogUseCase.record(MemberAuditEventFactory.registered(memberSnapshot));

        return created.getId();
    }
}
