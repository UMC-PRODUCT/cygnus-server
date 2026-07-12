package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationFormCommandService;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationFormValidationService;
import com.umc.product.recruiting.application.service.command.RecruitingFormSectionPolicyCommandService;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingFormSectionPolicyPersistenceAdapter.class,
    RecruitingApplicationFormValidationService.class,
    RecruitingApplicationFormCommandService.class,
    RecruitingFormSectionPolicyCommandService.class
})
class RecruitingApplicationFormPolicyConcurrencyTest {

    private static final Long REQUESTER_MEMBER_ID = 501L;
    private static final Long FORM_ID = 601L;
    private static final Long FORM_SECTION_ID = 701L;

    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;
    @Autowired
    RecruitingRoundPersistenceAdapter roundAdapter;
    @MockitoSpyBean
    RecruitingApplicationFormPersistenceAdapter formAdapter;
    @MockitoSpyBean
    RecruitingFormSectionPolicyPersistenceAdapter policyAdapter;
    @Autowired
    RecruitingApplicationFormCommandService formCommandService;
    @Autowired
    RecruitingFormSectionPolicyCommandService policyCommandService;
    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    ManageFormUseCase manageFormUseCase;
    @MockitoBean
    GetFormUseCase getFormUseCase;

    @Test
    @DisplayName("PostgreSQL에서 게시가 root lock을 잡으면 동시 section policy 추가는 게시 상태를 보고 거부된다")
    void publishSerializesConcurrentPolicyAddition() throws Exception {
        Fixture fixture = persistFixture();
        given(getFormUseCase.getFormWithStructure(FORM_ID)).willReturn(FormWithStructureInfo.builder()
            .formId(FORM_ID)
            .sections(List.of(SectionWithQuestions.builder()
                .sectionId(FORM_SECTION_ID)
                .questions(List.of())
                .build()))
            .build());
        CountDownLatch publishHasRootLock = new CountDownLatch(1);
        CountDownLatch allowPublish = new CountDownLatch(1);
        CountDownLatch policyAttemptsRootLock = new CountDownLatch(1);
        CountDownLatch policySaveAttempted = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("recruiting-form-publish")) {
                RecruitingApplicationForm form = (RecruitingApplicationForm) invocation.callRealMethod();
                publishHasRootLock.countDown();
                if (!allowPublish.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("게시 트랜잭션 진행 신호를 기다리는 데 실패했습니다.");
                }
                return form;
            }
            if (Thread.currentThread().getName().equals("recruiting-policy-add")) {
                policyAttemptsRootLock.countDown();
            }
            return invocation.callRealMethod();
        }).when(formAdapter).getByIdForUpdate(fixture.applicationFormId());
        doAnswer(invocation -> {
            policySaveAttempted.countDown();
            return invocation.callRealMethod();
        }).when(policyAdapter).save(any(RecruitingFormSectionPolicy.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> publishFuture = executor.submit(() -> {
                Thread.currentThread().setName("recruiting-form-publish");
                formCommandService.publish(PublishRecruitingApplicationFormCommand.builder()
                    .seasonId(fixture.seasonId())
                    .applicationFormId(fixture.applicationFormId())
                    .requesterMemberId(REQUESTER_MEMBER_ID)
                    .build());
                return true;
            });
            assertThat(publishHasRootLock.await(5, TimeUnit.SECONDS)).isTrue();

            Future<RecruitingErrorCode> policyFuture = executor.submit(() -> {
                Thread.currentThread().setName("recruiting-policy-add");
                try {
                    policyCommandService.addPolicy(AddRecruitingFormSectionPolicyCommand.builder()
                        .applicationFormId(fixture.applicationFormId())
                        .formSectionId(FORM_SECTION_ID)
                        .type(RecruitingFormSectionType.COMMON)
                        .build());
                    return null;
                } catch (RecruitingDomainException exception) {
                    return (RecruitingErrorCode) exception.getBaseCode();
                }
            });
            assertThat(policyAttemptsRootLock.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(policySaveAttempted.await(500, TimeUnit.MILLISECONDS)).isFalse();

            allowPublish.countDown();

            assertThat(publishFuture.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(policyFuture.get(5, TimeUnit.SECONDS))
                .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID_TRANSITION);
        } finally {
            allowPublish.countDown();
            executor.shutdownNow();
        }

        assertThat(applicationFormStatus(fixture.applicationFormId()))
            .isEqualTo(RecruitingApplicationFormStatus.PUBLISHED);
        assertThat(policyCount(fixture.applicationFormId())).isZero();
    }

    private Fixture persistFixture() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(801L, 802L));
            RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season));
            RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(round, FORM_ID));
            return new Fixture(season.getId(), form.getId());
        });
    }

    private RecruitingApplicationFormStatus applicationFormStatus(Long applicationFormId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> formAdapter.getById(applicationFormId).getStatus());
    }

    private int policyCount(Long applicationFormId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> policyAdapter.listByApplicationFormId(applicationFormId).size());
    }

    private record Fixture(Long seasonId, Long applicationFormId) {
    }
}
