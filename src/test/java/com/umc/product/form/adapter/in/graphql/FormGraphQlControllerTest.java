package com.umc.product.form.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
class FormGraphQlControllerTest {

    @Mock
    GetFormUseCase getFormUseCase;

    @InjectMocks
    FormGraphQlController controller;

    @Test
    @DisplayName("다른 회원의 draft Form은 구조를 조회하지 않고 null을 반환한다")
    void draftOwnedByAnotherMemberIsHidden() {
        given(getFormUseCase.findById(10L)).willReturn(Optional.of(formInfo(FormStatus.DRAFT, 2L)));

        var result = controller.form(new MemberPrincipal(1L), 10L);

        assertThat(result).isNull();
        verify(getFormUseCase, never()).getFormWithStructure(10L);
    }

    @Test
    @DisplayName("게시된 Form은 표준 구조로 반환한다")
    void publishedFormReturnsCanonicalStructure() {
        given(getFormUseCase.findById(10L)).willReturn(Optional.of(formInfo(FormStatus.PUBLISHED, 2L)));
        given(getFormUseCase.getFormWithStructure(10L)).willReturn(formStructure());

        var result = controller.form(new MemberPrincipal(1L), 10L);

        assertThat(result.formId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(FormStatus.PUBLISHED);
        assertThat(result.sections()).isEmpty();
    }

    private FormInfo formInfo(FormStatus status, Long ownerId) {
        return FormInfo.builder()
            .id(10L)
            .createdMemberId(ownerId)
            .title("설문")
            .status(status)
            .createdAt(Instant.EPOCH)
            .updatedAt(Instant.EPOCH)
            .build();
    }

    private FormWithStructureInfo formStructure() {
        return FormWithStructureInfo.builder()
            .formId(10L)
            .createdMemberId(2L)
            .title("설문")
            .status(FormStatus.PUBLISHED)
            .createdAt(Instant.EPOCH)
            .updatedAt(Instant.EPOCH)
            .sections(List.of())
            .build();
    }
}
