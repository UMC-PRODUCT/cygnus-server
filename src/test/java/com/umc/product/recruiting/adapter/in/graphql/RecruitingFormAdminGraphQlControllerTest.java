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
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;

@GraphQlTest(RecruitingFormAdminGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingFormAdminGraphQlControllerTest {

    @Autowired
    GraphQlTester graphQlTester;
    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @MockitoBean
    UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;
    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(40L), null, List.of())
        );
        given(getApplicationQueryUseCase.isRoundBelongsToSeason(20L, 10L)).willReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("지원 Form upsert Mutation은 section 정책과 조건부 이동 키를 함께 전달한다")
    void upsertFormStructure() {
        given(upsertFormUseCase.upsert(any())).willReturn(30L);

        graphQlTester.document("""
                mutation {
                  upsertRecruitingApplicationForm(
                    seasonId: 10,
                    roundId: 20,
                    input: {
                      description: "지원서",
                      sections: [{
                        clientKey: "common",
                        title: "공통",
                        type: COMMON,
                        questions: [{
                          type: RADIO,
                          title: "다음 단계",
                          required: true,
                          options: [{content: "계속", other: false, nextSectionKey: "track"}]
                        }]
                      }, {
                        clientKey: "track",
                        title: "트랙",
                        type: TRACK,
                        track: WEB_PRODUCT_ENGINEER,
                        questions: []
                      }]
                    }
                  ) { id }
                }
                """)
            .execute()
            .path("upsertRecruitingApplicationForm.id")
            .entity(String.class)
            .isEqualTo("30");

        ArgumentCaptor<UpsertRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(UpsertRecruitingApplicationFormCommand.class);
        then(upsertFormUseCase).should().upsert(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
        assertThat(captor.getValue().sections()).hasSize(2);
        assertThat(captor.getValue().sections().getFirst().questions().getFirst().options().getFirst()
            .nextSectionKey()).isEqualTo("track");
    }
}
