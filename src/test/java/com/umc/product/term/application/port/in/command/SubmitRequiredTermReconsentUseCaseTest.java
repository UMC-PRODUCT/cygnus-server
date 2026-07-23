package com.umc.product.term.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.term.application.port.in.command.dto.SubmitRequiredTermReconsentCommand;
import com.umc.product.term.application.port.out.LoadTermPort;
import com.umc.product.term.application.port.out.SaveTermConsentLogPort;
import com.umc.product.term.application.port.out.SaveTermConsentPort;
import com.umc.product.term.application.service.command.RequiredTermReconsentCommandService;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.TermConsentLog;
import com.umc.product.term.domain.enums.TermConsentStatus;
import com.umc.product.term.domain.enums.TermType;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

@ExtendWith(MockitoExtension.class)
class SubmitRequiredTermReconsentUseCaseTest {

    @Mock
    LoadTermPort loadTermPort;

    @Mock
    SaveTermConsentPort saveTermConsentPort;

    @Mock
    SaveTermConsentLogPort saveTermConsentLogPort;

    @InjectMocks
    RequiredTermReconsentCommandService sut;

    @Test
    @DisplayName("활성 필수 약관 재동의를 저장하고 법적 동의 로그를 남긴다")
    void saveActiveRequiredTerm() {
        Term term = term(10L, TermType.SERVICE, true);
        given(loadTermPort.findByIdWithSharedLock(10L)).willReturn(Optional.of(term));
        given(saveTermConsentPort.saveIfAbsent(any(TermConsent.class))).willReturn(true);

        sut.submitRequiredTermReconsent(SubmitRequiredTermReconsentCommand.of(100L, 10L));

        ArgumentCaptor<TermConsent> consentCaptor = ArgumentCaptor.forClass(TermConsent.class);
        then(saveTermConsentPort).should().saveIfAbsent(consentCaptor.capture());
        assertThat(consentCaptor.getValue().getMemberId()).isEqualTo(100L);
        assertThat(consentCaptor.getValue().getTermId()).isEqualTo(10L);

        ArgumentCaptor<TermConsentLog> logCaptor = ArgumentCaptor.forClass(TermConsentLog.class);
        then(saveTermConsentLogPort).should().save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(TermConsentStatus.AGREED);
    }

    @Test
    @DisplayName("이미 저장된 재동의를 반복 제출하면 성공하고 중복 로그를 남기지 않는다")
    void repeatedRequestIsIdempotent() {
        Term term = term(10L, TermType.SERVICE, true);
        given(loadTermPort.findByIdWithSharedLock(10L)).willReturn(Optional.of(term));
        given(saveTermConsentPort.saveIfAbsent(any(TermConsent.class))).willReturn(false);

        sut.submitRequiredTermReconsent(SubmitRequiredTermReconsentCommand.of(100L, 10L));

        then(saveTermConsentPort).should().saveIfAbsent(any(TermConsent.class));
        then(saveTermConsentLogPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("비활성 필수 약관은 재동의를 거부한다")
    void rejectInactiveRequiredTerm() {
        Term term = term(10L, TermType.SERVICE, true);
        term.deactivate();
        given(loadTermPort.findByIdWithSharedLock(10L)).willReturn(Optional.of(term));

        assertInvalidReconsentTerm(term.getId());
    }

    @Test
    @DisplayName("활성 선택 약관은 재동의를 거부한다")
    void rejectOptionalTerm() {
        Term term = term(20L, TermType.MARKETING, false);
        given(loadTermPort.findByIdWithSharedLock(20L)).willReturn(Optional.of(term));

        assertInvalidReconsentTerm(term.getId());
    }

    private void assertInvalidReconsentTerm(Long termId) {
        assertThatThrownBy(() -> sut.submitRequiredTermReconsent(
            SubmitRequiredTermReconsentCommand.of(100L, termId)
        ))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.INVALID_RECONSENT_TERM);
        then(saveTermConsentPort).should(never()).saveIfAbsent(any());
        then(saveTermConsentLogPort).shouldHaveNoInteractions();
    }

    private Term term(Long id, TermType type, boolean required) {
        Term term = Term.builder()
            .type(type)
            .link("https://example.com/terms/" + type.name().toLowerCase())
            .required(required)
            .build();
        ReflectionTestUtils.setField(term, "id", id);
        return term;
    }
}
