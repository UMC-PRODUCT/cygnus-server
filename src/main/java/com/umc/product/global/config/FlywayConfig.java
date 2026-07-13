package com.umc.product.global.config;

import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlywayConfig {

    /**
     * {@code CREATE INDEX CONCURRENTLY} migration이 transaction-level advisory lock과 서로 기다리지 않도록
     * session-level lock을 사용한다. Flyway instance 간 migration 직렬성은 session lock으로 유지된다.
     */
    @Bean
    public FlywayConfigurationCustomizer flywayPostgresqlSessionLockCustomizer() {
        return configuration -> configuration.getPluginRegister()
            .getPlugin(PostgreSQLConfigurationExtension.class)
            .setTransactionalLock(false);
    }
}
