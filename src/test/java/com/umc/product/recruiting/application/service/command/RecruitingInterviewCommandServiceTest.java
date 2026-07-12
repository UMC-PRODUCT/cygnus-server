package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.domain.RecruitingApplication;

@ExtendWith(MockitoExtension.class)
class RecruitingInterviewCommandServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;

    @Mock
    FindRecruitingScheduleOverlapPort findScheduleOverlapPort;

    @Mock
    SendEmailPort sendEmailPort;

    RecruitingInterviewCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingInterviewCommandService(
            loadApplicationPort,
            saveApplicationPort,
            findScheduleOverlapPort,
            sendEmailPort
        );
    }

    @Test
    @DisplayName("면접 일정 후보 조회는 기존 overlap 경계에 위임한다")
    void 면접_일정_후보_조회는_기존_overlap_경계에_위임한다() {
        List<RecruitingInterviewScheduleCandidate> expected = List.of(
            new RecruitingInterviewScheduleCandidate(
                Instant.parse("2026-08-12T01:00:00Z"),
                Instant.parse("2026-08-12T01:30:00Z"),
                3
            )
        );
        given(findScheduleOverlapPort.findOverlaps(100L, List.of(1L, 2L))).willReturn(expected);

        List<RecruitingInterviewScheduleCandidate> result = sut.findScheduleCandidates(
            FindRecruitingInterviewScheduleCandidatesCommand.builder()
                .formId(100L)
                .formResponseIds(List.of(1L, 2L))
                .build()
        );

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("legacy 면접 안내 API는 기존 메일 포트 호출을 유지한다")
    void legacy_면접_안내_API는_기존_메일_포트_호출을_유지한다() {
        RecruitingApplication application = org.mockito.Mockito.mock(RecruitingApplication.class);
        given(application.getId()).willReturn(900L);
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        sut.sendGuide(SendRecruitingInterviewGuideCommand.builder()
            .applicationId(900L)
            .recipientEmail("recipient@example.com")
            .startsAt(Instant.parse("2026-08-12T01:00:00Z"))
            .location("온라인")
            .build());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sendEmailPort).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("recipient@example.com");
        assertThat(captor.getValue().htmlBody()).contains("지원서 ID: 900", "온라인");
    }

    @Test
    @DisplayName("면접 생략은 지원서 도메인 전이를 저장한다")
    void 면접_생략은_지원서_도메인_전이를_저장한다() {
        RecruitingApplication application = org.mockito.Mockito.mock(RecruitingApplication.class);
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        sut.skip(SkipRecruitingInterviewCommand.builder()
            .applicationId(900L)
            .skippedByMemberId(20L)
            .reason("면접 없음")
            .build());

        verify(application).skipInterview(20L, "면접 없음");
        verify(saveApplicationPort).save(application);
    }
}
