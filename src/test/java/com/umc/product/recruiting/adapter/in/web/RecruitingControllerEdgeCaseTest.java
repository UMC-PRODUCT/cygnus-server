package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitAnonymousRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateAnonymousRecruitingApplicationRequest;
import com.umc.product.recruiting.application.port.in.command.CancelAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RecruitingControllerEdgeCaseTest {

    @Mock GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;
    @Mock SearchPublicRecruitingRoundUseCase searchPublicRoundUseCase;
    @Mock GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;
    @Mock CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;
    @Mock UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;
    @Mock SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;
    @Mock CancelAnonymousRecruitingApplicationUseCase cancelAnonymousApplicationUseCase;
    @Mock CreateRecruitingApplicationDraftUseCase createDraftUseCase;
    @Mock UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;
    @Mock SubmitRecruitingApplicationUseCase submitUseCase;
    @Mock CancelRecruitingApplicationUseCase cancelUseCase;
    @Mock HttpServletRequest servletRequest;

    @InjectMocks RecruitingPublicController publicController;
    @InjectMocks RecruitingApplicationController applicationController;

    @Test
    @DisplayName("익명 지원서 수정·제출은 정규화 DTO와 원격 IP를 command에 전달한다")
    void delegateAnonymousUpdateAndSubmit() {
        given(updateAnonymousApplicationUseCase.updateAnonymous(any()))
            .willReturn(org.mockito.Mockito.mock(RecruitingApplicationInfo.class));
        given(submitAnonymousApplicationUseCase.submitAnonymous(any()))
            .willReturn(org.mockito.Mockito.mock(RecruitingApplicationInfo.class));
        given(servletRequest.getRemoteAddr()).willReturn("127.0.0.1");
        UpdateAnonymousRecruitingApplicationRequest update =
            new UpdateAnonymousRecruitingApplicationRequest(
                "current@example.com",
                "A1B2C3",
                "지원자",
                "new@example.com",
                ChallengerTrack.PLAN,
                null,
                List.of()
            );

        assertThat(publicController.updateAnonymousApplication(update)).isNotNull();
        assertThat(publicController.submitAnonymousApplication(
            new SubmitAnonymousRecruitingApplicationRequest("new@example.com", "A1B2C3"),
            servletRequest
        )).isNotNull();
        then(updateAnonymousApplicationUseCase).should().updateAnonymous(any());
        then(submitAnonymousApplicationUseCase).should().submitAnonymous(any());
    }

    @Test
    @DisplayName("로그인 지원서 제출·철회의 생략 가능한 body는 기본 request로 변환한다")
    void defaultNullableSubmitAndCancelRequests() {
        given(servletRequest.getRemoteAddr()).willReturn("127.0.0.1");
        given(submitUseCase.submit(any())).willReturn(org.mockito.Mockito.mock(RecruitingApplicationInfo.class));
        given(cancelUseCase.cancel(any())).willReturn(org.mockito.Mockito.mock(RecruitingApplicationInfo.class));
        MemberPrincipal principal = new MemberPrincipal(10L);

        assertThat(applicationController.submit(principal, 20L, null, servletRequest)).isNotNull();
        assertThat(applicationController.cancel(principal, 20L, null)).isNotNull();
    }
}
