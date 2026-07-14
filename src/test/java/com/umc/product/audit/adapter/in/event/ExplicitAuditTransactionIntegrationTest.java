package com.umc.product.audit.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.audit.adapter.out.persistence.AuditLogJpaRepository;
import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("명시 감사 recorder 트랜잭션 계약")
class ExplicitAuditTransactionIntegrationTest extends IntegrationTestSupport {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    RecordAuditLogUseCase recordAuditLogUseCase;

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("명시 SUCCESS는 외부 비즈니스 트랜잭션이 롤백되면 저장하지 않는다")
    void explicitSuccessDoesNotSurviveOuterRollback() {
        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> {
            recordAuditLogUseCase.record(command("explicit-success-rollback", AuditOutcome.SUCCESS));
            throw new IllegalStateException("비즈니스 롤백 probe");
        })).isInstanceOf(IllegalStateException.class);

        await().during(Duration.ofMillis(300)).atMost(ASYNC_TIMEOUT)
            .untilAsserted(() -> assertThat(auditLogJpaRepository.count()).isZero());
    }

    @Test
    @DisplayName("명시 FAILURE는 외부 비즈니스 트랜잭션이 롤백되어도 저장한다")
    void explicitFailureSurvivesOuterRollback() {
        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> {
            recordAuditLogUseCase.record(command("explicit-failure-rollback", AuditOutcome.FAILURE));
            throw new IllegalStateException("비즈니스 롤백 probe");
        })).isInstanceOf(IllegalStateException.class);

        await().atMost(ASYNC_TIMEOUT)
            .untilAsserted(() -> assertThat(auditLogJpaRepository.count()).isOne());
        assertThat(auditLogJpaRepository.findAll().getFirst().getOutcome())
            .isEqualTo(AuditOutcome.FAILURE);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private static RecordAuditLogCommand command(String targetId, AuditOutcome outcome) {
        return RecordAuditLogCommand.of(
            Domain.MEMBER,
            AuditAction.UPDATE,
            "Member",
            targetId,
            7L,
            "명시 감사 트랜잭션 probe",
            Map.of("target", Map.of("type", "Member", "id", targetId)),
            "198.51.100.7",
            outcome,
            AuditSource.EXPLICIT_RECORDER,
            "request-transaction-probe",
            "trace-transaction-probe"
        );
    }
}
