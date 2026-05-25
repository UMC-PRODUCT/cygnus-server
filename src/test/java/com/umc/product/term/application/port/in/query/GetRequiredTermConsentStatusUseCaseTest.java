package com.umc.product.term.application.port.in.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.term.application.port.in.query.dto.RequiredTermConsentStatusInfo;
import com.umc.product.term.application.port.out.LoadTermConsentPort;
import com.umc.product.term.application.port.out.LoadTermPort;
import com.umc.product.term.application.service.query.RequiredTermConsentStatusQueryService;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.enums.TermType;

@ExtendWith(MockitoExtension.class)
class GetRequiredTermConsentStatusUseCaseTest {

    @Mock
    LoadTermPort loadTermPort;

    @Mock
    LoadTermConsentPort loadTermConsentPort;

    @InjectMocks
    RequiredTermConsentStatusQueryService sut;

    @Test
    @DisplayName("현재 활성 필수 약관 중 미동의 약관을 반환한다")
    void 현재_활성_필수_약관_중_미동의_약관을_반환한다() {
        // given
        Term serviceTerm = createTerm(1L, TermType.SERVICE);
        Term privacyTerm = createTerm(2L, TermType.PRIVACY);
        given(loadTermPort.findAllActiveRequired()).willReturn(List.of(serviceTerm, privacyTerm));
        given(loadTermConsentPort.listByMemberIdAndTermIds(100L, List.of(1L, 2L))).willReturn(List.of(
            createConsent(100L, serviceTerm)
        ));

        // when
        RequiredTermConsentStatusInfo result = sut.getRequiredTermConsentStatus(100L);

        // then
        assertThat(result.needsReconsent()).isTrue();
        assertThat(result.missingRequiredTerms())
            .extracting(term -> term.id())
            .containsExactly(2L);
        assertThat(result.agreedRequiredTermIds()).containsExactly(1L);
    }

    @Test
    @DisplayName("현재 활성 필수 약관을 모두 동의한 경우 재동의가 필요하지 않다")
    void 현재_활성_필수_약관을_모두_동의한_경우_재동의가_필요하지_않다() {
        // given
        Term serviceTerm = createTerm(1L, TermType.SERVICE);
        Term privacyTerm = createTerm(2L, TermType.PRIVACY);
        given(loadTermPort.findAllActiveRequired()).willReturn(List.of(serviceTerm, privacyTerm));
        given(loadTermConsentPort.listByMemberIdAndTermIds(100L, List.of(1L, 2L))).willReturn(List.of(
            createConsent(100L, serviceTerm),
            createConsent(100L, privacyTerm)
        ));

        // when
        RequiredTermConsentStatusInfo result = sut.getRequiredTermConsentStatus(100L);

        // then
        assertThat(result.needsReconsent()).isFalse();
        assertThat(result.missingRequiredTerms()).isEmpty();
        assertThat(result.agreedRequiredTermIds()).containsExactly(1L, 2L);
    }

    private TermConsent createConsent(Long memberId, Term term) {
        return TermConsent.builder()
            .memberId(memberId)
            .termId(term.getId())
            .termType(term.getType())
            .agreedAt(Instant.parse("2026-05-26T00:00:00Z"))
            .build();
    }

    private Term createTerm(Long id, TermType type) {
        Term term = Term.builder()
            .type(type)
            .link("https://example.com/terms/" + type.name().toLowerCase())
            .required(true)
            .build();
        ReflectionTestUtils.setField(term, "id", id);
        return term;
    }
}
