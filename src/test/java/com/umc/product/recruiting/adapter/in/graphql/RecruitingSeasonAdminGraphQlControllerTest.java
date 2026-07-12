package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;

@GraphQlTest(RecruitingAdminGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingSeasonAdminGraphQlControllerTest {

    @Autowired
    GraphQlTester graphQlTester;
    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @MockitoBean
    GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    @MockitoBean
    CreateRecruitingSeasonUseCase createSeasonUseCase;
    @MockitoBean
    UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    @MockitoBean
    ReplaceRecruitingSeasonTrackQuotasUseCase replaceQuotasUseCase;
    @MockitoBean
    CreateRecruitingRoundUseCase createRoundUseCase;
    @MockitoBean
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    @MockitoBean
    UpdateRecruitingRoundUseCase updateRoundUseCase;
    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;
    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(40L), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GraphQL 시즌 생성은 0명을 포함한 쿼터를 command로 전달한다")
    void createSeasonWithQuotas() {
        given(createSeasonUseCase.createSeason(any())).willReturn(10L);

        graphQlTester.document("""
                mutation {
                  createRecruitingSeason(input: {
                    gisuId: 11,
                    schoolId: 22,
                    quotas: [
                      {track: PLAN, targetCount: 0},
                      {track: DESIGN, targetCount: 4}
                    ]
                  }) { id }
                }
                """)
            .execute()
            .path("createRecruitingSeason.id")
            .entity(String.class)
            .isEqualTo("10");

        ArgumentCaptor<CreateRecruitingSeasonCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingSeasonCommand.class);
        then(createSeasonUseCase).should().createSeason(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
        assertThat(captor.getValue().quotas()).hasSize(2);
        assertThat(captor.getValue().quotas().getFirst().targetCount()).isZero();
    }

    @Test
    @DisplayName("GraphQL 쿼터 교체는 seasonId와 트랙 설정을 command로 전달한다")
    void replaceSeasonQuotas() {
        graphQlTester.document("""
                mutation {
                  replaceRecruitingSeasonTrackQuotas(
                    seasonId: 10,
                    input: {quotas: [{track: WEB_PRODUCT_ENGINEER, targetCount: 3}]}
                  )
                }
                """)
            .execute()
            .path("replaceRecruitingSeasonTrackQuotas")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<ReplaceRecruitingSeasonTrackQuotasCommand> captor =
            ArgumentCaptor.forClass(ReplaceRecruitingSeasonTrackQuotasCommand.class);
        then(replaceQuotasUseCase).should().replaceQuotas(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().quotas().getFirst().track())
            .isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }
}
