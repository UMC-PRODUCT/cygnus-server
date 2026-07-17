package com.umc.product.form.application.service.query;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.anonymous;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static com.umc.product.form.application.service.FormAccessTestFixtures.responseActor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormResponseQueryServiceTest {

    private static final Long FORM_ID = 100L;

    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    GetAnswerUseCase getAnswerUseCase;
    @Mock
    SecureTokenGenerator secureTokenGenerator;
    @Mock
    FormOwnershipAccessService ownershipAccessService;

    @InjectMocks
    FormResponseQueryService sut;

    @Test
    @DisplayName("listDraftByRespondentMemberId: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void listDraftByRespondentMemberId_null_예외() {
        assertThatThrownBy(() -> sut.listDraftByRespondent(List.of(owner(FORM_ID)), anonymous()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormResponsePort).should(never()).findAllDraftByRespondentMemberId(any());
    }

    @Test
    @DisplayName("findDraftByFormIdAndRespondentMemberId: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void findDraftByFormIdAndRespondentMemberId_null_예외() {
        assertThatThrownBy(() -> sut.findDraftByRespondent(owner(FORM_ID), anonymous()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormResponsePort).should(never()).findDraftByFormIdAndRespondentMemberId(anyLong(), any());
    }

    @Test
    @DisplayName("findSubmittedByFormIdAndRespondentMemberId: respondentMemberId=null 이면 RESPONDENT_MEMBER_ID_REQUIRED")
    void findSubmittedByFormIdAndRespondentMemberId_null_예외() {
        assertThatThrownBy(() -> sut.findSubmittedByRespondent(owner(FORM_ID), anonymous()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONDENT_MEMBER_ID_REQUIRED);

        then(loadFormResponsePort).should(never()).findSubmittedByFormIdAndRespondentMemberId(anyLong(), any());
    }

    // ============================================================
    //          익명 응답 조회 (Phase 4.7)
    // ============================================================

    @Test
    @DisplayName("findByAccessKey: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void findByAccessKey_null_예외() {
        assertThatThrownBy(() -> sut.findByAccessKey(owner(FORM_ID), anonymous()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findByAccessKeyHash(any());
    }

    @Test
    @DisplayName("findByAccessKey: 매칭 실패면 Optional.empty")
    void findByAccessKey_매칭_실패_empty() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThat(sut.findByAccessKey(owner(FORM_ID), responseActor(rawKey))).isEmpty();
    }

    @Test
    @DisplayName("findByAccessKey: 기명 응답이 매칭되면 방어 목적으로 Optional.empty")
    void findByAccessKey_기명_응답이면_empty() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedResponse = anonymousDraftWithMember(200L);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findByAccessKeyHash(hash)).willReturn(Optional.of(namedResponse));

        assertThat(sut.findByAccessKey(owner(FORM_ID), responseActor(rawKey))).isEmpty();
    }

    @Test
    @DisplayName("findByAccessKey: 익명 응답이 매칭되면 반환")
    void findByAccessKey_익명_응답이면_반환() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse anonymousResponse = anonymousResponseWithId(300L);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findByAccessKeyHash(hash)).willReturn(Optional.of(anonymousResponse));

        assertThat(sut.findByAccessKey(owner(FORM_ID), responseActor(rawKey))).isPresent();
    }

    @Test
    @DisplayName("getResponseWithAnswersByAccessKey: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void getResponseWithAnswersByAccessKey_null_예외() {
        assertThatThrownBy(() -> sut.getResponseWithAnswersByAccessKey(owner(FORM_ID), anonymous()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);

        then(loadFormResponsePort).should(never()).findByAccessKeyHash(any());
    }

    @Test
    @DisplayName("getResponseWithAnswersByAccessKey: 매칭 실패면 FORM_RESPONSE_NOT_FOUND")
    void getResponseWithAnswersByAccessKey_매칭_실패_NOT_FOUND() {
        String rawKey = "raw";
        String hash = "hash";
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findByAccessKeyHash(hash)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getResponseWithAnswersByAccessKey(
            owner(FORM_ID), responseActor(rawKey)
        ))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("getResponseWithAnswersByAccessKey: 기명 응답이 매칭되면 FORM_RESPONSE_NOT_FOUND")
    void getResponseWithAnswersByAccessKey_기명_응답이면_NOT_FOUND() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse namedResponse = anonymousDraftWithMember(200L);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findByAccessKeyHash(hash)).willReturn(Optional.of(namedResponse));

        assertThatThrownBy(() -> sut.getResponseWithAnswersByAccessKey(
            owner(FORM_ID), responseActor(rawKey)
        ))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("getResponseWithAnswersByAccessKey: 익명 응답이 매칭되면 상세 반환")
    void getResponseWithAnswersByAccessKey_익명_응답이면_반환() {
        String rawKey = "raw";
        String hash = "hash";
        FormResponse anonymousResponse = anonymousResponseWithId(300L);
        given(secureTokenGenerator.sha256Hex(rawKey)).willReturn(hash);
        given(loadFormResponsePort.findByAccessKeyHash(hash)).willReturn(Optional.of(anonymousResponse));
        given(getAnswerUseCase.listByFormResponseIdAsAnonymous(
            owner(FORM_ID), responseActor(rawKey), 300L
        )).willReturn(List.of());

        assertThat(sut.getResponseWithAnswersByAccessKey(
            owner(FORM_ID), responseActor(rawKey)
        )).isNotNull();
    }

    // ============================================================
    //          ID 기반 상세 조회의 익명 응답 방어
    // ============================================================

    @Test
    @DisplayName("findResponseWithAnswers: 익명 응답이면 Optional.empty")
    void findResponseWithAnswers_익명_응답이면_empty() {
        FormResponse anonymousResponse = anonymousResponseWithId(300L);
        given(loadFormResponsePort.findById(300L)).willReturn(Optional.of(anonymousResponse));

        assertThat(sut.findResponseWithAnswers(
            owner(FORM_ID), actor(200L), 300L
        )).isEmpty();

        then(getAnswerUseCase).should(never()).listByFormResponseId(any(), any(), anyLong());
    }

    @Test
    @DisplayName("findResponseWithAnswers: 기명 응답이면 반환")
    void findResponseWithAnswers_기명_응답이면_반환() {
        FormResponse namedResponse = namedResponseWithId(300L, 200L);
        given(loadFormResponsePort.findById(300L)).willReturn(Optional.of(namedResponse));
        given(getAnswerUseCase.listByFormResponseId(
            owner(FORM_ID), actor(200L), 300L
        )).willReturn(List.of());

        assertThat(sut.findResponseWithAnswers(
            owner(FORM_ID), actor(200L), 300L
        )).isPresent();
    }

    @Test
    @DisplayName("getResponseWithAnswers: 익명 응답이면 FORM_RESPONSE_NOT_FOUND")
    void getResponseWithAnswers_익명_응답이면_NOT_FOUND() {
        FormResponse anonymousResponse = anonymousResponseWithId(300L);
        given(loadFormResponsePort.findById(300L)).willReturn(Optional.of(anonymousResponse));

        assertThatThrownBy(() -> sut.getResponseWithAnswers(
            owner(FORM_ID), actor(200L), 300L
        ))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("findResponsesWithAnswers: 익명 응답은 결과 map 에서 제외")
    void findResponsesWithAnswers_익명_응답은_제외() {
        FormResponse named = namedResponseWithId(300L, 200L);
        FormResponse anonymous = anonymousResponseWithId(301L);
        given(loadFormResponsePort.listByIdsWithForm(java.util.Set.of(300L, 301L)))
            .willReturn(List.of(named, anonymous));
        given(getAnswerUseCase.listByFormResponseIds(
            List.of(owner(FORM_ID)), actor(200L), java.util.Set.of(300L, 301L)
        ))
            .willReturn(java.util.Map.of());

        var result = sut.findResponsesWithAnswers(
            List.of(owner(FORM_ID)), actor(200L), java.util.Set.of(300L, 301L)
        );

        assertThat(result).containsOnlyKeys(300L);
    }

    private FormResponse anonymousDraftWithMember(Long memberId) {
        Form form = Form.createDraft("폼", 1L, false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        form.publish();
        FormResponse response = FormResponse.createDraft(form, memberId);
        return response;
    }

    private FormResponse namedResponseWithId(Long id, Long memberId) {
        FormResponse response = anonymousDraftWithMember(memberId);
        ReflectionTestUtils.setField(response, "id", id);
        return response;
    }

    private FormResponse anonymousResponseWithId(Long id) {
        Form form = Form.createDraft("폼", 1L, false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        form.publish();
        FormResponse response = FormResponse.createAnonymousDraft(form, "hash-value");
        ReflectionTestUtils.setField(response, "id", id);
        return response;
    }
}
