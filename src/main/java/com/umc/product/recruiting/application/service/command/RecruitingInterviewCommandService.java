package com.umc.product.recruiting.application.service.command;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.domain.RecruitingApplication;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewCommandService implements
    SkipRecruitingInterviewUseCase,
    FindRecruitingInterviewScheduleCandidatesUseCase,
    SendRecruitingInterviewGuideUseCase {

    private static final String FROM_ADDRESS = "no-reply@umc.product";
    private static final String FROM_DISPLAY_NAME = "UMC";

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final FindRecruitingScheduleOverlapPort findScheduleOverlapPort;
    private final SendEmailPort sendEmailPort;

    @Override
    public void skip(SkipRecruitingInterviewCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        application.skipInterview(command.skippedByMemberId(), command.reason());
        saveApplicationPort.save(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitingInterviewScheduleCandidate> findScheduleCandidates(
        FindRecruitingInterviewScheduleCandidatesCommand command
    ) {
        return findScheduleOverlapPort.findOverlaps(command.formId(), command.formResponseIds());
    }

    @Override
    public void sendGuide(SendRecruitingInterviewGuideCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        String body = buildGuideBody(application, command.startsAt(), command.location());
        // TODO(#1147): Thymeleaf 템플릿 기반 HTML SendEmailUseCase가 병합되면 SendEmailPort 직접 호출을 교체한다.
        sendEmailPort.send(new EmailMessage(
            FROM_ADDRESS,
            FROM_DISPLAY_NAME,
            command.recipientEmail(),
            "[UMC] 면접 안내",
            body
        ));
    }

    private String buildGuideBody(RecruitingApplication application, Instant startsAt, String location) {
        return """
            <p>UMC 면접 안내입니다.</p>
            <p>지원서 ID: %s</p>
            <p>면접 시간: %s</p>
            <p>장소: %s</p>
            """.formatted(application.getId(), startsAt, location);
    }
}
