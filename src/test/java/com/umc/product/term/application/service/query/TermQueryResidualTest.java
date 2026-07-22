package com.umc.product.term.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.term.application.port.out.LoadTermConsentPort;
import com.umc.product.term.application.port.out.LoadTermPort;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.enums.TermType;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

@DisplayName("약관 query service 잔여 엣지 케이스")
class TermQueryResidualTest {

    @Test
    @DisplayName("회원 ID가 없거나 동의 이력이 비어 있으면 port 오조회 없이 실패 또는 빈 목록을 반환한다")
    void 약관_동의_조회_입력과_빈_이력을_처리한다() {
        LoadTermConsentPort consentPort = mock(LoadTermConsentPort.class);
        LoadTermPort termPort = mock(LoadTermPort.class);
        TermAgreementQueryService sut = new TermAgreementQueryService(consentPort, termPort);
        given(consentPort.findByMemberId(10L)).willReturn(List.of());

        assertThatThrownBy(() -> sut.getAgreedTermsByMemberId(null))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.MEMBER_ID_REQUIRED);
        assertThat(sut.getAgreedTermsByMemberId(10L)).isEmpty();
        then(termPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("동의 이력의 약관 row가 부분 누락되면 과거 동의를 다른 버전으로 재해석하지 않고 실패한다")
    void 동의한_약관_row_누락을_검증한다() {
        LoadTermConsentPort consentPort = mock(LoadTermConsentPort.class);
        LoadTermPort termPort = mock(LoadTermPort.class);
        TermAgreementQueryService sut = new TermAgreementQueryService(consentPort, termPort);
        TermConsent consent = consent(10L, 1L, TermType.SERVICE);
        given(consentPort.findByMemberId(10L)).willReturn(List.of(consent));
        given(termPort.listByIds(List.of(1L))).willReturn(List.of());

        assertThatThrownBy(() -> sut.getAgreedTermsByMemberId(10L))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.TERMS_NOT_FOUND);
    }

    @Test
    @DisplayName("필수 약관이 없으면 동의 port를 조회하지 않고 재동의 불필요를 반환한다")
    void 활성_필수_약관_부재를_단축한다() {
        LoadTermPort termPort = mock(LoadTermPort.class);
        LoadTermConsentPort consentPort = mock(LoadTermConsentPort.class);
        RequiredTermConsentStatusQueryService sut =
            new RequiredTermConsentStatusQueryService(termPort, consentPort);
        given(termPort.findAllActiveRequired()).willReturn(List.of());

        assertThatThrownBy(() -> sut.getRequiredTermConsentStatus(null))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.MEMBER_ID_REQUIRED);
        assertThat(sut.getRequiredTermConsentStatus(10L).needsReconsent()).isFalse();
        then(consentPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("활성 필수 약관 ID는 중복을 제거한 Set으로 반환한다")
    void 필수_약관_ID_중복을_제거한다() {
        LoadTermPort termPort = mock(LoadTermPort.class);
        TermQueryService sut = new TermQueryService(termPort);
        Term service = term(1L, TermType.SERVICE);
        Term duplicate = term(1L, TermType.SERVICE);
        given(termPort.findAllActiveRequired()).willReturn(List.of(service, duplicate));

        assertThat(sut.getRequiredTermIds()).containsExactly(1L);
    }

    @Test
    @DisplayName("약관 예외의 사용자 지정 메시지를 보존한다")
    void 사용자_지정_예외_메시지를_보존한다() {
        TermDomainException exception =
            new TermDomainException(TermErrorCode.TERM_PERMISSION_DENIED, "지원하지 않는 권한");

        assertThat(exception.getMessage()).contains("지원하지 않는 권한");
        assertThat(exception.getBaseCode()).isEqualTo(TermErrorCode.TERM_PERMISSION_DENIED);
    }

    private Term term(Long id, TermType type) {
        Term value = Term.builder().type(type).link("https://example.com/terms").required(true).build();
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private TermConsent consent(Long memberId, Long termId, TermType type) {
        return TermConsent.builder()
            .memberId(memberId)
            .termId(termId)
            .termType(type)
            .agreedAt(Instant.parse("2026-07-22T00:00:00Z"))
            .build();
    }
}
