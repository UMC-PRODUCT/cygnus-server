package com.umc.product.storage.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

class DisabledFileUsageRegistryReadinessAdapterTest {

    @Test
    @DisplayName("실제 cutover adapter가 없으면 파일 사용 registry readiness는 DISABLED다")
    void 실제_cutover_adapter가_없으면_파일_사용_registry_readiness는_DISABLED다() {
        // given
        DisabledFileUsageRegistryReadinessAdapter adapter = new DisabledFileUsageRegistryReadinessAdapter();

        // when & then
        assertThat(adapter.getStatus()).isEqualTo(FileUsageRegistryStatus.DISABLED);
        assertThat(adapter.isReady()).isFalse();
    }
}
