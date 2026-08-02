package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;

@GraphQlTest(RecruitingScheduleGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
@DisplayName("RecruitingGraphQlExceptionAdvice")
class RecruitingGraphQlExceptionAdviceTest {

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @MockitoBean
    GetRecruitingInterviewScheduleUseCase getInterviewScheduleUseCase;

    @MockitoBean
    ManageRecruitingInterviewScheduleUseCase manageInterviewScheduleUseCase;

    @MockitoBean
    ManageRecruitingInterviewSessionUseCase manageInterviewSessionUseCase;

    @MockitoBean
    ConfirmRecruitingInterviewSchedulesUseCase confirmInterviewSchedulesUseCase;

    @MockitoBean
    GetRecruitingInterviewSessionUseCase getInterviewSessionUseCase;

    @MockitoBean
    GetRecruitingInterviewScheduleBoardUseCase getInterviewScheduleBoardUseCase;

    @MockitoBean
    SkipRecruitingInterviewUseCase skipInterviewUseCase;

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
    @DisplayName("잘못된 Instant 일정은 GraphQL 입력 오류로 거부한다")
    void 잘못된_Instant_일정은_GraphQL_입력_오류로_거부한다() {
        graphQlTester.document("""
                mutation {
                  confirmRecruitingInterviewSchedule(
                    applicationId: 20,
                    input: {
                      startsAt: "not-an-instant",
                      endsAt: "2026-08-11T01:00:00Z",
                      contactSnapshot: "운영진 문의"
                    }
                  )
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        then(manageInterviewScheduleUseCase).shouldHaveNoInteractions();
    }
}
