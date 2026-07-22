package com.umc.product.term.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.term.adapter.in.web.dto.request.CreateTermRequest;
import com.umc.product.term.adapter.in.web.dto.response.RequiredTermConsentStatusResponse;
import com.umc.product.term.adapter.in.web.dto.response.TermResponse;
import com.umc.product.term.application.port.in.command.ManageTermUseCase;
import com.umc.product.term.application.port.in.command.dto.CreateTermCommand;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.application.port.in.query.dto.RequiredTermConsentStatusInfo;
import com.umc.product.term.application.port.in.query.dto.TermInfo;
import com.umc.product.term.domain.enums.TermType;

@DisplayName("TermController 잔여 변환 계약")
class TermControllerResidualTest {

    @Test
    @DisplayName("타입·ID 조회 결과와 필수 약관 누락을 응답 DTO로 변환한다")
    void 약관_조회_응답을_변환한다() {
        GetTermUseCase getUseCase = mock(GetTermUseCase.class);
        GetRequiredTermConsentStatusUseCase statusUseCase = mock(GetRequiredTermConsentStatusUseCase.class);
        TermController sut = new TermController(getUseCase, statusUseCase, mock(ManageTermUseCase.class));
        TermInfo info = new TermInfo(1L, "https://example.com/terms", true, TermType.SERVICE);
        given(getUseCase.getTermsByType(TermType.SERVICE)).willReturn(info);
        given(getUseCase.getTermsById(1L)).willReturn(info);
        given(statusUseCase.getRequiredTermConsentStatus(10L))
            .willReturn(new RequiredTermConsentStatusInfo(true, List.of(info)));

        TermResponse byType = sut.getTerms(TermType.SERVICE);
        TermResponse byId = sut.getTermsById(1L);
        RequiredTermConsentStatusResponse status =
            sut.getMyRequiredTermConsentStatus(new MemberPrincipal(10L));

        assertThat(byType).isEqualTo(byId);
        assertThat(byType.id()).isEqualTo(1L);
        assertThat(status.needsReconsent()).isTrue();
        assertThat(status.missingRequiredTerms()).containsExactly(byType);
    }

    @Test
    @DisplayName("생성 요청의 link·필수 여부·타입을 command로 정확히 변환한다")
    void 약관_생성_요청을_command로_변환한다() {
        ManageTermUseCase manageUseCase = mock(ManageTermUseCase.class);
        TermController sut = new TermController(
            mock(GetTermUseCase.class), mock(GetRequiredTermConsentStatusUseCase.class), manageUseCase);
        CreateTermRequest request =
            new CreateTermRequest("https://example.com/privacy", false, TermType.PRIVACY);
        given(manageUseCase.createTerms(org.mockito.ArgumentMatchers.any())).willReturn(20L);

        assertThat(sut.createTerms(request)).isEqualTo(20L);
        ArgumentCaptor<CreateTermCommand> captor = ArgumentCaptor.forClass(CreateTermCommand.class);
        then(manageUseCase).should().createTerms(captor.capture());
        assertThat(captor.getValue())
            .returns("https://example.com/privacy", CreateTermCommand::link)
            .returns(false, CreateTermCommand::required)
            .returns(TermType.PRIVACY, CreateTermCommand::type);
    }
}
