package com.umc.product.registry.adapter.in.runner;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.umc.product.registry.adapter.in.runner.RegistryBackfillJobRunner.ProcessTerminator;
import com.umc.product.registry.application.port.out.RegistryAdvisoryLockPort;
import com.umc.product.registry.application.port.out.RegistryControlPort;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.application.service.RegistryBackfillCoordinator;

@Profile("registry-backfill")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RegistryBackfillProperties.class)
public class RegistryBackfillRunnerConfiguration {

    @Bean
    @ConditionalOnMissingBean(RegistryBackfillCoordinator.class)
    RegistryBackfillCoordinator registryBackfillCoordinator(
        List<RegistryRolloutPort> rolloutPorts,
        RegistryControlPort controlPort,
        RegistryAdvisoryLockPort lockPort,
        Clock clock,
        RegistryBackfillProperties properties
    ) {
        return new RegistryBackfillCoordinator(
            rolloutPorts,
            controlPort,
            lockPort,
            clock,
            properties.getBatchSize(),
            properties.getDetailLimit()
        );
    }

    @Bean
    @ConditionalOnProperty(name = "app.registry.backfill.action")
    RegistryBackfillJobRunner registryBackfillJobRunner(
        RegistryBackfillCoordinator coordinator,
        RegistryBackfillProperties properties,
        ProcessTerminator processTerminator
    ) {
        return new RegistryBackfillJobRunner(
            coordinator,
            properties.getAction(),
            processTerminator
        );
    }

    @Bean
    ProcessTerminator registryBackfillProcessTerminator(
        ConfigurableApplicationContext applicationContext
    ) {
        return () -> SpringApplication.exit(applicationContext);
    }
}
