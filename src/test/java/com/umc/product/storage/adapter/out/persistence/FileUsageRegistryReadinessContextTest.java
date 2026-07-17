package com.umc.product.storage.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.support.IntegrationTestSupport;

class FileUsageRegistryReadinessContextTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("일반 SpringBootTest context에는 readiness fallback 빈이 하나만 등록되고 기본 상태는 DISABLED다")
    void 일반_SpringBootTest_context에는_readiness_fallback_빈이_하나만_등록되고_기본_상태는_DISABLED다() {
        // given
        Map<String, FileUsageRegistryReadinessPort> readinessBeans = applicationContext
            .getBeansOfType(FileUsageRegistryReadinessPort.class);

        // when
        assertThat(readinessBeans).hasSize(1);

        // then
        FileUsageRegistryReadinessPort readinessPort = readinessBeans.values().iterator().next();
        assertThat(readinessPort.getStatus()).isEqualTo(FileUsageRegistryStatus.DISABLED);
        assertThat(readinessPort.isReady()).isFalse();
    }
}
