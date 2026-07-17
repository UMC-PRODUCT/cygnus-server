package com.umc.product.form.application.service.query;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static com.umc.product.form.application.service.FormAccessTestFixtures.responseActor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;

@ExtendWith(MockitoExtension.class)
class FormResponseOptionalQueryOwnershipTest {

    private static final Long FORM_ID = 100L;
    private static final Long MEMBER_ID = 200L;
    private static final String RAW_KEY = "raw-key";

    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    LoadAnswerPort loadAnswerPort;
    @Mock
    GetAnswerUseCase getAnswerUseCase;
    @Mock
    FormOwnershipAccessService ownershipAccessService;
    @Mock
    SecureTokenGenerator secureTokenGenerator;

    private FormResponseQueryService formResponseQueryService;
    private AnswerQueryService answerQueryService;

    @BeforeEach
    void setUp() {
        formResponseQueryService = new FormResponseQueryService(
            loadFormResponsePort,
            getAnswerUseCase,
            ownershipAccessService,
            secureTokenGenerator
        );
        answerQueryService = new AnswerQueryService(
            loadAnswerPort,
            loadFormResponsePort,
            ownershipAccessService,
            secureTokenGenerator
        );
    }

    @Test
    @DisplayName("missing response Optional도 expected owner policy를 검증한다")
    void missing_response도_expected_owner를_검증한다() {
        // given
        given(loadFormResponsePort.findById(1L)).willReturn(Optional.empty());

        // when
        var result = formResponseQueryService.findById(
            owner(FORM_ID), actor(MEMBER_ID), 1L
        );

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(actor(MEMBER_ID));
    }

    @Test
    @DisplayName("missing response-with-answers Optional도 expected owner policy를 검증한다")
    void missing_response_with_answers도_expected_owner를_검증한다() {
        // given
        given(loadFormResponsePort.findById(1L)).willReturn(Optional.empty());

        // when
        var result = formResponseQueryService.findResponseWithAnswers(
            owner(FORM_ID), actor(MEMBER_ID), 1L
        );

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(actor(MEMBER_ID));
    }

    @Test
    @DisplayName("missing access-key response Optional도 expected owner policy를 검증한다")
    void missing_access_key_response도_expected_owner를_검증한다() {
        // given
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn("hash");
        given(loadFormResponsePort.findByAccessKeyHash("hash")).willReturn(Optional.empty());

        // when
        var result = formResponseQueryService.findByAccessKey(
            owner(FORM_ID), responseActor(RAW_KEY)
        );

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(responseActor(RAW_KEY));
    }

    @Test
    @DisplayName("missing answer Optional도 expected owner policy를 검증한다")
    void missing_answer도_expected_owner를_검증한다() {
        // given
        given(loadAnswerPort.findById(2L)).willReturn(Optional.empty());

        // when
        var result = answerQueryService.findById(owner(FORM_ID), actor(MEMBER_ID), 2L);

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(actor(MEMBER_ID));
    }

    @Test
    @DisplayName("missing anonymous answer Optional도 expected owner policy를 검증한다")
    void missing_anonymous_answer도_expected_owner를_검증한다() {
        // given
        given(loadAnswerPort.findById(2L)).willReturn(Optional.empty());

        // when
        var result = answerQueryService.findByIdAsAnonymous(
            owner(FORM_ID), responseActor(RAW_KEY), 2L
        );

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(responseActor(RAW_KEY));
    }

    @Test
    @DisplayName("missing named response answer 목록도 expected owner policy를 검증한다")
    void missing_named_response_answer_목록도_expected_owner를_검증한다() {
        // given
        given(loadFormResponsePort.findById(1L)).willReturn(Optional.empty());

        // when
        var result = answerQueryService.listByFormResponseId(
            owner(FORM_ID), actor(MEMBER_ID), 1L
        );

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(actor(MEMBER_ID));
    }

    @Test
    @DisplayName("missing anonymous response answer 목록도 expected owner policy를 검증한다")
    void missing_anonymous_response_answer_목록도_expected_owner를_검증한다() {
        // given
        given(loadFormResponsePort.findById(1L)).willReturn(Optional.empty());

        // when
        var result = answerQueryService.listByFormResponseIdAsAnonymous(
            owner(FORM_ID), responseActor(RAW_KEY), 1L
        );

        // then
        assertThat(result).isEmpty();
        verifyExpectedOwnerRead(responseActor(RAW_KEY));
    }

    private void verifyExpectedOwnerRead(
        com.umc.product.form.application.port.in.FormActorContext actorContext
    ) {
        then(ownershipAccessService).should().requireRead(
            FORM_ID, owner(FORM_ID), actorContext, FormOperation.READ
        );
    }
}
