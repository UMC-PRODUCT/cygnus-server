package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;

@GraphQlTest(RecruitingDecisionGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingDecisionGraphQlControllerTest {

    private static final Long REQUESTER_ID = 40L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @MockitoBean
    DecideRecruitingDocumentUseCase decideDocumentUseCase;

    @MockitoBean
    DecideRecruitingFinalUseCase decideFinalUseCase;

    @MockitoBean
    PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;

    @MockitoBean
    CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;

    @MockitoBean
    ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
        given(getApplicationQueryUseCase.isApplicationBelongsToSeason(20L, 10L)).willReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("최종 합격 Mutation은 acceptedTrack과 CurrentMember를 public UseCase에 전달한다")
    void 최종_합격_Mutation은_acceptedTrack과_CurrentMember를_public_UseCase에_전달한다() {
        graphQlTester.document("""
                mutation ($seasonId: ID!, $applicationId: ID!) {
                  decideRecruitingFinal(input: {
                    seasonId: $seasonId
                    applicationId: $applicationId
                    decision: PASS
                    acceptedTrack: DESIGN
                    reason: "최종 합격"
                  }) { success }
                }
                """)
            .variable("seasonId", globalId(GlobalIdTypes.RECRUITING_SEASON, 10L))
            .variable("applicationId", globalId(GlobalIdTypes.RECRUITING_APPLICATION, 20L))
            .execute()
            .path("decideRecruitingFinal.success")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<DecideRecruitingFinalCommand> captor =
            ArgumentCaptor.forClass(DecideRecruitingFinalCommand.class);
        then(decideFinalUseCase).should().decideFinal(captor.capture());
        assertThat(captor.getValue().acceptedTrack()).isEqualTo(ChallengerTrack.DESIGN);
        assertThat(captor.getValue().decidedByMemberId()).isEqualTo(REQUESTER_ID);
    }

    @Test
    @DisplayName("READY Mutation은 CurrentMember를 executor로 전달한다")
    void READY_Mutation은_CurrentMember를_executor로_전달한다() {
        graphQlTester.document("""
                mutation ($input: PrepareRecruitingRegistrationInput!) {
                  prepareRecruitingRegistration(input: $input) { success }
                }
                """)
            .variable("input", registrationInput())
            .execute()
            .path("prepareRecruitingRegistration.success")
            .entity(Boolean.class)
            .isEqualTo(true);

        then(prepareRegistrationUseCase).should().prepareRegistration(
            PrepareRecruitingRegistrationCommand.of(20L, REQUESTER_ID)
        );
    }

    @Test
    @DisplayName("등록 관리 권한이 없으면 READY UseCase를 호출하지 않는다")
    void 등록_관리_권한이_없으면_READY_UseCase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("denied"))
            .given(checkPermissionUseCase)
            .checkOrThrow(eq(REQUESTER_ID), any());

        graphQlTester.document("""
                mutation ($input: PrepareRecruitingRegistrationInput!) {
                  prepareRecruitingRegistration(input: $input) { success }
                }
                """)
            .variable("input", registrationInput())
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).anySatisfy(error -> assertThat(error.getExtensions())
                    .containsEntry("code", CommonErrorCode.FORBIDDEN.getCode()));
            });

        then(prepareRegistrationUseCase).shouldHaveNoInteractions();
    }

    private java.util.Map<String, Object> registrationInput() {
        return java.util.Map.of(
            "seasonId", globalId(GlobalIdTypes.RECRUITING_SEASON, 10L),
            "applicationId", globalId(GlobalIdTypes.RECRUITING_APPLICATION, 20L)
        );
    }

    private String globalId(String typeName, Long rawId) {
        return GlobalId.encode(typeName, rawId);
    }
}
