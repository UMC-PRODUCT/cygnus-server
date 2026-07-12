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
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingFormSectionPolicyUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

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
    GetRecruitingFormQueryUseCase getFormQueryUseCase;

    @MockitoBean
    LinkRecruitingApplicationFormUseCase linkFormUseCase;

    @MockitoBean
    ManageRecruitingFormSectionPolicyUseCase manageFormSectionPolicyUseCase;

    @MockitoBean
    PublishRecruitingApplicationFormUseCase publishFormUseCase;

    @MockitoBean
    CloseRecruitingApplicationFormUseCase closeFormUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(40L), null, List.of())
        );
        given(getFormQueryUseCase.isApplicationFormBelongsToSeason(30L, 10L)).willReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("트랙 폼 정책 Mutation은 track list 모델의 단일 섹션 정책을 전달한다")
    void 트랙_폼_정책_Mutation은_track_list_모델의_단일_섹션_정책을_전달한다() {
        given(manageFormSectionPolicyUseCase.addPolicy(any())).willReturn(80L);

        graphQlTester.document("""
                mutation {
                  addRecruitingFormSectionPolicy(
                    seasonId: 10,
                    applicationFormId: 30,
                    input: {formSectionId: 90, type: TRACK, track: WEB_PRODUCT_ENGINEER}
                  ) { id }
                }
                """)
            .execute()
            .path("addRecruitingFormSectionPolicy.id")
            .entity(String.class)
            .isEqualTo("80");

        ArgumentCaptor<AddRecruitingFormSectionPolicyCommand> captor =
            ArgumentCaptor.forClass(AddRecruitingFormSectionPolicyCommand.class);
        then(manageFormSectionPolicyUseCase).should().addPolicy(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(RecruitingFormSectionType.TRACK);
        assertThat(captor.getValue().track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("지원 Form 연결 Mutation은 검증한 시즌 ID를 command로 전달한다")
    void linkFormBindsValidatedSeason() {
        given(getApplicationQueryUseCase.isRoundBelongsToSeason(20L, 10L)).willReturn(true);
        given(linkFormUseCase.link(any())).willReturn(30L);

        graphQlTester.document("""
                mutation {
                  linkRecruitingApplicationForm(
                    seasonId: 10,
                    roundId: 20,
                    input: {formId: 300}
                  ) { id }
                }
                """)
            .execute()
            .path("linkRecruitingApplicationForm.id")
            .entity(String.class)
            .isEqualTo("30");

        ArgumentCaptor<LinkRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(LinkRecruitingApplicationFormCommand.class);
        then(linkFormUseCase).should().link(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().roundId()).isEqualTo(20L);
    }
}
