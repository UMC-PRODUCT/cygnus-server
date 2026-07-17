package com.umc.product.form.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("StandaloneFormOwnerPolicy")
class StandaloneFormOwnerPolicyTest {

    private static final Long FORM_ID = 10L;
    private static final Long CREATOR_ID = 20L;
    private static final FormOwnerReference OWNER = FormOwnerReference.of(
        FORM_ID, "form.standalone", FORM_ID.toString(), "default"
    );

    private LoadFormPort loadFormPort;
    private StandaloneFormOwnerPolicy sut;

    @BeforeEach
    void setUp() {
        loadFormPort = mock(LoadFormPort.class);
        sut = new StandaloneFormOwnerPolicy(loadFormPort);
    }

    @Test
    @DisplayName("creator만 구조 관리, 게시, 삭제, 응답 결과 읽기를 할 수 있다")
    void creator만_관리_operation을_할_수_있다() {
        Form form = publishedForm();
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(form));

        assertThat(sut.allows(OWNER, FormOperation.MANAGE_STRUCTURE, authenticated(CREATOR_ID))).isTrue();
        assertThat(sut.allows(OWNER, FormOperation.PUBLISH, authenticated(CREATOR_ID))).isTrue();
        assertThat(sut.allows(OWNER, FormOperation.DELETE, authenticated(CREATOR_ID))).isTrue();
        assertThat(sut.allows(OWNER, FormOperation.READ_RESPONSES, authenticated(CREATOR_ID))).isTrue();
        assertThat(sut.allows(OWNER, FormOperation.MANAGE_STRUCTURE, authenticated(99L))).isFalse();
        assertThat(sut.allows(OWNER, FormOperation.READ_RESPONSES, FormActorContext.anonymous())).isFalse();
    }

    @Test
    @DisplayName("published standalone Form의 READ와 RESPOND는 anonymous actor에게도 열린다")
    void published_form의_READ와_RESPOND는_anonymous에게도_열린다() {
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(publishedForm()));

        assertThat(sut.allows(OWNER, FormOperation.READ, FormActorContext.anonymous())).isTrue();
        assertThat(sut.allows(OWNER, FormOperation.RESPOND, FormActorContext.anonymous())).isTrue();
    }

    @Test
    @DisplayName("draft standalone Form은 creator만 읽고 anonymous 응답은 거부한다")
    void draft_form은_creator만_읽고_anonymous_응답은_거부한다() {
        Form draft = Form.createDraft("draft", CREATOR_ID);
        ReflectionTestUtils.setField(draft, "id", FORM_ID);
        given(loadFormPort.findById(FORM_ID)).willReturn(Optional.of(draft));

        assertThat(sut.allows(OWNER, FormOperation.READ, authenticated(CREATOR_ID))).isTrue();
        assertThat(sut.allows(OWNER, FormOperation.READ, FormActorContext.anonymous())).isFalse();
        assertThat(sut.allows(OWNER, FormOperation.RESPOND, FormActorContext.anonymous())).isFalse();
    }

    @Test
    @DisplayName("standalone canonical owner tuple이 아니면 거부한다")
    void standalone_canonical_owner_tuple이_아니면_거부한다() {
        FormOwnerReference forged = FormOwnerReference.of(
            FORM_ID, "form.standalone", "999", "default"
        );

        assertThat(sut.allows(forged, FormOperation.READ, FormActorContext.anonymous())).isFalse();
    }

    private Form publishedForm() {
        Form form = Form.createPublished(CREATOR_ID, "published", false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        return form;
    }

    private FormActorContext authenticated(Long memberId) {
        return FormActorContext.authenticated(memberId);
    }
}
