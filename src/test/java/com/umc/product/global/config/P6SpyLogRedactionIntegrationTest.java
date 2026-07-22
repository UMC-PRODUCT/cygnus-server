package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.p6spy.engine.logging.P6LogFactory;
import com.p6spy.engine.spy.P6ModuleManager;
import com.p6spy.engine.spy.P6SpyFactory;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import com.umc.product.maintenance.adapter.out.persistence.MaintenanceWindowRepository;
import com.umc.product.support.IntegrationTestSupport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ContextConfiguration(initializers = P6SpyLogRedactionIntegrationTest.P6SpyLoggingInitializer.class)
@DisplayName("P6Spy SQL 로그 보안 통합 테스트")
class P6SpyLogRedactionIntegrationTest extends IntegrationTestSupport {

    private static final String PROBE_EMAIL = "sql-probe@example.invalid";
    private static final String PROBE_KEY = "R4SAFE";
    private static String previousP6SpyModuleList;
    private static String previousP6SpyAppender;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MaintenanceWindowRepository maintenanceWindowRepository;

    @Test
    @DisplayName("실제 PostgreSQL INSERT와 SELECT 로그는 바인딩 값을 노출하지 않는다")
    void 실제_PostgreSQL_INSERT와_SELECT_로그는_바인딩_값을_노출하지_않는다() {
        ch.qos.logback.classic.Logger p6spyLogger =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("p6spy");
        Level previousLevel = p6spyLogger.getLevel();
        boolean previousAdditive = p6spyLogger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        p6spyLogger.addAppender(appender);
        p6spyLogger.setLevel(Level.INFO);
        p6spyLogger.setAdditive(false);

        try {
            jdbcTemplate.execute("drop table if exists sql_redaction_probe");
            jdbcTemplate.execute("create table sql_redaction_probe (email_address text, access_key text)");
            jdbcTemplate.update(
                "insert into sql_redaction_probe (email_address, access_key) values (?, ?)",
                PROBE_EMAIL,
                PROBE_KEY
            );
            Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sql_redaction_probe where email_address = ? and access_key = ?",
                Integer.class,
                PROBE_EMAIL,
                PROBE_KEY
            );

            assertThat(count).isEqualTo(1);
            String formattedSql = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
            assertThat(formattedSql)
                .contains("sql_redaction_probe", "email_address", "access_key", "?")
                .doesNotContain(PROBE_EMAIL, PROBE_KEY);
        } finally {
            jdbcTemplate.execute("drop table if exists sql_redaction_probe");
            p6spyLogger.detachAppender(appender);
            appender.stop();
            p6spyLogger.setLevel(previousLevel);
            p6spyLogger.setAdditive(previousAdditive);
        }
    }

    @Test
    @DisplayName("명시적인 제외 tag가 붙은 maintenance polling SQL은 P6Spy 로그에 남지 않는다")
    void 명시적인_제외_tag가_붙은_maintenance_polling_SQL은_P6Spy_로그에_남지_않는다() {
        ch.qos.logback.classic.Logger p6spyLogger =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("p6spy");
        Level previousLevel = p6spyLogger.getLevel();
        boolean previousAdditive = p6spyLogger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        p6spyLogger.addAppender(appender);
        p6spyLogger.setLevel(Level.INFO);
        p6spyLogger.setAdditive(false);

        try {
            jdbcTemplate.queryForObject("select 1 as p6spy_visible_probe", Integer.class);
            maintenanceWindowRepository.findActiveAt(Instant.now());

            String formattedSql = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
            assertThat(formattedSql)
                .contains("p6spy_visible_probe")
                .doesNotContain("maintenance_window");
        } finally {
            p6spyLogger.detachAppender(appender);
            appender.stop();
            p6spyLogger.setLevel(previousLevel);
            p6spyLogger.setAdditive(previousAdditive);
        }
    }

    @AfterAll
    static void restoreP6SpyConfiguration() {
        restoreSystemProperty("p6spy.config.modulelist", previousP6SpyModuleList);
        restoreSystemProperty("p6spy.config.appender", previousP6SpyAppender);
        P6ModuleManager.getInstance().reload();
    }

    private static void restoreSystemProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
            return;
        }
        System.setProperty(name, value);
    }

    static final class P6SpyLoggingInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            previousP6SpyModuleList = System.getProperty("p6spy.config.modulelist");
            previousP6SpyAppender = System.getProperty("p6spy.config.appender");
            System.setProperty(
                "p6spy.config.modulelist",
                String.join(",", P6SpyFactory.class.getName(), P6LogFactory.class.getName())
            );
            System.setProperty("p6spy.config.appender", Slf4JLogger.class.getName());
            P6ModuleManager.getInstance().reload();
        }
    }
}
