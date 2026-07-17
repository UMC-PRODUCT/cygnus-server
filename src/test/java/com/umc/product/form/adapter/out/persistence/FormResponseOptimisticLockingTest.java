package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

@PersistenceAdapterTest
@DisplayName("FormResponse 낙관적 락 (@Version)")
class FormResponseOptimisticLockingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동일 FormResponse 에 대한 동시 수정이 발생하면 뒤늦은 flush 는 OptimisticLockException 으로 거부된다")
    void staleWriterCannotOverwriteConcurrentlyUpdatedFormResponse() {
        Long formResponseId = persistDraftFormResponse();
        FormResponse staleCopy = findDetached(formResponseId);
        assertThat(staleCopy.getVersion()).isZero();

        // 다른 세션이 먼저 커밋해 version=1 로 진행시킨다.
        EntityManager competingEntityManager = entityManagerFactory.createEntityManager();
        competingEntityManager.getTransaction().begin();
        FormResponse competing = competingEntityManager.find(FormResponse.class, formResponseId);
        competing.updateLastSavedAt(Instant.now().plusSeconds(1));
        competingEntityManager.getTransaction().commit();
        competingEntityManager.close();

        // stale 카피 (여전히 version=0) 로 쓰기를 시도하면 CAS 가 실패한다.
        EntityManager staleEntityManager = entityManagerFactory.createEntityManager();
        staleEntityManager.getTransaction().begin();
        staleCopy.updateLastSavedAt(Instant.now().plusSeconds(2));

        assertThatThrownBy(() -> {
            staleEntityManager.merge(staleCopy);
            staleEntityManager.flush();
        }).isInstanceOf(OptimisticLockException.class);

        staleEntityManager.getTransaction().rollback();
        staleEntityManager.close();
    }

    private Long persistDraftFormResponse() {
        EntityManager setupEntityManager = entityManagerFactory.createEntityManager();
        setupEntityManager.getTransaction().begin();
        Form form = Form.createDraft("동시성 테스트 폼", 1L);
        setupEntityManager.persist(form);
        FormResponse formResponse = FormResponse.createDraft(form, 1L);
        setupEntityManager.persist(formResponse);
        setupEntityManager.getTransaction().commit();
        Long formResponseId = formResponse.getId();
        setupEntityManager.close();
        return formResponseId;
    }

    private FormResponse findDetached(Long formResponseId) {
        EntityManager workerEntityManager = entityManagerFactory.createEntityManager();
        FormResponse formResponse = workerEntityManager.find(FormResponse.class, formResponseId);
        workerEntityManager.close();
        return formResponse;
    }
}
