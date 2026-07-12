package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormResponseQueryServiceTest {

    private static final Long FORM_ID = 100L;

    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    GetAnswerUseCase getAnswerUseCase;

    @InjectMocks
    FormResponseQueryService sut;

    @Test
    @DisplayName("listDraftByRespondentMemberId: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void listDraftByRespondentMemberId_null_예외() {
        assertThatThrownBy(() -> sut.listDraftByRespondentMemberId(null))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormResponsePort).should(never()).findAllDraftByRespondentMemberId(any());
    }

    @Test
    @DisplayName("findDraftByFormIdAndRespondentMemberId: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void findDraftByFormIdAndRespondentMemberId_null_예외() {
        assertThatThrownBy(() -> sut.findDraftByFormIdAndRespondentMemberId(FORM_ID, null))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormResponsePort).should(never()).findDraftByFormIdAndRespondentMemberId(anyLong(), any());
    }

    @Test
    @DisplayName("findSubmittedByFormIdAndRespondentMemberId: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void findSubmittedByFormIdAndRespondentMemberId_null_예외() {
        assertThatThrownBy(() -> sut.findSubmittedByFormIdAndRespondentMemberId(FORM_ID, null))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormResponsePort).should(never()).findSubmittedByFormIdAndRespondentMemberId(anyLong(), any());
    }
}
