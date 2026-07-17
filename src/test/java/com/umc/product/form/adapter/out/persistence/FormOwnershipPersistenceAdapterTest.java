package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import(FormOwnershipPersistenceAdapter.class)
@DisplayName("FormOwnershipPersistenceAdapter")
class FormOwnershipPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    FormOwnershipPersistenceAdapter sut;

    @Test
    @DisplayName("form ID와 owner tuple을 저장하고 다시 조회한다")
    void form_ID와_owner_tuple을_저장하고_다시_조회한다() {
        Form form = persistForm();
        FormOwnerReference reference = reference(form, "project.application-form", "10", "default");

        sut.save(reference);
        em.flush();
        em.clear();

        assertThat(sut.findByFormId(form.getId()))
            .get()
            .extracting(FormOwnership::toReference)
            .isEqualTo(reference);
        assertThat(sut.findDistinctNamespaces()).containsExactly("project.application-form");
    }

    @Test
    @DisplayName("동일 binding은 idempotent하게 한 행만 유지한다")
    void 동일_binding은_idempotent하게_한_행만_유지한다() {
        Form form = persistForm();
        FormOwnerReference reference = reference(form, "project.application-form", "10", "default");

        sut.save(reference);
        sut.save(reference);
        em.flush();

        Number count = (Number) em.getEntityManager()
            .createNativeQuery("select count(*) from form_ownership where form_id = :formId")
            .setParameter("formId", form.getId())
            .getSingleResult();
        assertThat(count.longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("동일 form의 owner transfer는 거부한다")
    void 동일_form의_owner_transfer는_거부한다() {
        Form form = persistForm();
        sut.save(reference(form, "project.application-form", "10", "default"));

        assertThatThrownBy(() -> sut.save(reference(form, "project.application-form", "11", "default")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("동일 owner slot을 다른 form에 중복 바인딩하지 않는다")
    void 동일_owner_slot을_다른_form에_중복_바인딩하지_않는다() {
        Form first = persistForm();
        Form second = persistForm();
        sut.save(reference(first, "project.application-form", "10", "default"));

        assertThatThrownBy(() -> sut.save(reference(second, "project.application-form", "10", "default")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("form 삭제 시 ownership도 cascade 삭제한다")
    void form_삭제_시_ownership도_cascade_삭제한다() {
        Form form = persistForm();
        sut.save(reference(form, "form.standalone", form.getId().toString(), "default"));
        em.flush();

        em.remove(em.find(Form.class, form.getId()));
        em.flush();
        em.clear();

        assertThat(sut.findByFormId(form.getId())).isEmpty();
    }

    @Test
    @DisplayName("ownership row를 lock 조회한다")
    void ownership_row를_lock_조회한다() {
        Form form = persistForm();
        FormOwnerReference reference = reference(form, "form.standalone", form.getId().toString(), "default");
        sut.save(reference);
        em.flush();
        em.clear();

        assertThat(sut.findByFormIdForUpdate(form.getId()))
            .get()
            .extracting(FormOwnership::toReference)
            .isEqualTo(reference);
    }

    @Test
    @DisplayName("잘못된 grammar는 DB CHECK에서 거부한다")
    void 잘못된_grammar는_DB_CHECK에서_거부한다() {
        Form form = persistForm();

        assertThatThrownBy(() -> em.getEntityManager()
            .createNativeQuery("""
                insert into form_ownership(form_id, namespace, owner_resource_key, slot, created_at, updated_at)
                values (:formId, 'Project.application-form', '10', 'default', now(), now())
                """)
            .setParameter("formId", form.getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    private Form persistForm() {
        Form form = em.persist(Form.createDraft("소유권 테스트", 1L));
        em.flush();
        return form;
    }

    private FormOwnerReference reference(Form form, String namespace, String resourceKey, String slot) {
        return FormOwnerReference.of(form.getId(), namespace, resourceKey, slot);
    }
}
