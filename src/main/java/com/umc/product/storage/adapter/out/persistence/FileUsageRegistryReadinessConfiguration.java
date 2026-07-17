package com.umc.product.storage.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;

@Configuration(proxyBeanMethods = false)
public class FileUsageRegistryReadinessConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileUsageRegistryReadinessPort.class)
    DisabledFileUsageRegistryReadinessAdapter disabledFileUsageRegistryReadinessAdapter() {
        return new DisabledFileUsageRegistryReadinessAdapter();
    }
}
