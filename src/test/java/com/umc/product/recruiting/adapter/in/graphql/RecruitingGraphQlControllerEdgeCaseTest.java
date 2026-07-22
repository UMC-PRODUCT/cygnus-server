package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CancelAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

@ExtendWith(MockitoExtension.class)
class RecruitingGraphQlControllerEdgeCaseTest {

    @Mock GetRecruitingFormQueryUseCase getFormQueryUseCase;
    @Mock GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @Mock CreateRecruitingApplicationDraftUseCase createDraftUseCase;
    @Mock UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;
    @Mock SubmitRecruitingApplicationUseCase submitApplicationUseCase;
    @Mock CancelRecruitingApplicationUseCase cancelApplicationUseCase;
    @Mock GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;
    @Mock CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;
    @Mock UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;
    @Mock SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;
    @Mock CancelAnonymousRecruitingApplicationUseCase cancelAnonymousApplicationUseCase;
    @Mock SearchPublicRecruitingRoundUseCase searchPublicRoundUseCase;
    @Mock RecruitingGraphQlPermissionSupport permissionSupport;
    @InjectMocks RecruitingGraphQlController sut;

    @Test
    @DisplayName("로그인 지원서 제출·철회 Mutation은 input 생략 시 기본 request를 사용한다")
    void defaultNullableMutationInputs() {
        MemberPrincipal principal = new MemberPrincipal(10L);
        given(permissionSupport.currentMemberId(principal)).willReturn(10L);
        given(submitApplicationUseCase.submit(any()))
            .willReturn(org.mockito.Mockito.mock(RecruitingApplicationInfo.class));
        given(cancelApplicationUseCase.cancel(any()))
            .willReturn(org.mockito.Mockito.mock(RecruitingApplicationInfo.class));

        assertThat(sut.submitRecruitingApplication(principal, 20L, null)).isNotNull();
        assertThat(sut.cancelRecruitingApplication(principal, 20L, null)).isNotNull();
    }
}
