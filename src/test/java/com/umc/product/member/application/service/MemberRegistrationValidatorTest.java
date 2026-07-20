package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRegistrationValidator")
class MemberRegistrationValidatorTest {

    @Mock
    GetTermUseCase getTermUseCase;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    MemberRegistrationValidator sut;

    @BeforeEach
    void setUp() {
        sut = new MemberRegistrationValidator(getTermUseCase, getSchoolUseCase, getFileUseCase);
    }

    @Nested
    @DisplayName("프로필 이미지")
    class ProfileImage {

        @Test
        @DisplayName("프로필 이미지 ID가 없으면 파일 저장소를 조회하지 않는다")
        void null이면_파일을_조회하지_않는다() {
            sut.validateProfileImageExists(null);

            then(getFileUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("프로필 이미지 ID가 있으면 존재하지 않을 때 예외를 위임한다")
        void ID가_있으면_파일_검증을_위임한다() {
            sut.validateProfileImageExists("profile-image-id");

            then(getFileUseCase).should().throwIfNotExists("profile-image-id");
        }
    }

    @Test
    @DisplayName("학교 존재 검증은 학교 상세 조회에 위임한다")
    void 학교_존재_검증을_위임한다() {
        sut.validateSchoolExists(10L);

        then(getSchoolUseCase).should().getSchoolDetail(10L);
    }

    @Nested
    @DisplayName("필수 약관")
    class MandatoryTerms {

        @Test
        @DisplayName("필수 약관을 모두 동의하면 선택 약관 상태와 관계없이 통과한다")
        void 필수_약관을_모두_동의하면_통과한다() {
            given(getTermUseCase.getRequiredTermIds()).willReturn(Set.of(1L, 2L));
            List<TermConsents> consents = List.of(
                new TermConsents(1L, true),
                new TermConsents(2L, true),
                new TermConsents(3L, false),
                new TermConsents(1L, true)
            );

            assertThatCode(() -> sut.validateMandatoryTermsAgreed(consents)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("필수 약관 중 하나라도 없거나 미동의하면 거부한다")
        void 필수_약관이_누락되면_거부한다() {
            given(getTermUseCase.getRequiredTermIds()).willReturn(Set.of(1L, 2L));
            List<TermConsents> consents = List.of(
                new TermConsents(1L, true),
                new TermConsents(2L, false)
            );

            assertThatThrownBy(() -> sut.validateMandatoryTermsAgreed(consents))
                .isInstanceOf(TermDomainException.class)
                .extracting("baseCode")
                .isEqualTo(TermErrorCode.MANDATORY_TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("필수 약관이 없으면 빈 동의 목록도 통과한다")
        void 필수_약관이_없으면_빈_목록도_통과한다() {
            given(getTermUseCase.getRequiredTermIds()).willReturn(Set.of());

            assertThatCode(() -> sut.validateMandatoryTermsAgreed(List.of())).doesNotThrowAnyException();
        }
    }
}
