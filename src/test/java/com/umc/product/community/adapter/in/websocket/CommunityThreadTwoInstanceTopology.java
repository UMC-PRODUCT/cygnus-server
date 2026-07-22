package com.umc.product.community.adapter.in.websocket;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import com.umc.product.UmcProductApplication;
import com.umc.product.global.event.application.service.EventOutboxRelayService;
import com.umc.product.global.websocket.relay.StompBrokerRelayMonitor;

final class CommunityThreadTwoInstanceTopology implements AutoCloseable {

    private static final String POSTGIS_IMAGE_PROPERTY = "umc.test.postgis.image";
    private static final String POSTGIS_IMAGE_ENV = "UMC_TEST_POSTGIS_IMAGE";
    private static final String POSTGIS_IMAGE = "postgis/postgis:18-3.6";
    private static final String ARM64_POSTGIS_IMAGE = "umc-product-postgis-test:18.2-postgis";
    private static final Path ARM64_POSTGIS_DOCKERFILE = Path.of("docker/test/postgis/Dockerfile");
    private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:4.1-management");
    private static final int STOMP_PORT = 61613;
    private static final String BROKER_USER = "relay-e2e";
    private static final String BROKER_PASSWORD = "relay-e2e-password";
    private static final String JWT_ACCESS_TOKEN_SECRET =
        "community-thread-e2e-access-key-material-20260718";
    private static final String JWT_REFRESH_TOKEN_SECRET =
        "community-thread-e2e-refresh-key-material-20260718";
    private static final String JWT_OAUTH_VERIFICATION_TOKEN_SECRET =
        "community-thread-e2e-oauth-key-material-20260718";
    private static final String JWT_EMAIL_VERIFICATION_TOKEN_SECRET =
        "community-thread-e2e-email-key-material-20260718";
    private static final String JWT_SSO_LOGIN_TOKEN_SECRET =
        "community-thread-e2e-sso-key-material-20260718";
    private static final Duration INITIAL_RELAY_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration RELAY_TRANSITION_TIMEOUT = Duration.ofSeconds(30);

    private final PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(postgisImage())
        .withDatabaseName("community_thread_two_instance")
        .withUsername("community_e2e")
        .withPassword("community_e2e_password");
    private final GenericContainer<?> rabbitmq = new GenericContainer<>(RABBITMQ_IMAGE)
        .withEnv("RABBITMQ_DEFAULT_USER", BROKER_USER)
        .withEnv("RABBITMQ_DEFAULT_PASS", BROKER_PASSWORD)
        .withExposedPorts(STOMP_PORT)
        .withCommand(
            "sh",
            "-c",
            "rabbitmq-plugins enable --offline rabbitmq_stomp && exec rabbitmq-server"
        )
        .waitingFor(Wait.forListeningPort())
        .withStartupTimeout(Duration.ofMinutes(3));

    private AppInstance appA;
    private AppInstance appB;
    private boolean relayPaused;

    static CommunityThreadTwoInstanceTopology start() {
        CommunityThreadTwoInstanceTopology topology = new CommunityThreadTwoInstanceTopology();
        try {
            topology.postgis.start();
            topology.rabbitmq.start();
            topology.appA = topology.startApp("community-relay-a", true);
            topology.appB = topology.startApp("community-relay-b", false);
            topology.requireInitiallyAvailable(topology.appA);
            topology.requireInitiallyAvailable(topology.appB);
            return topology;
        } catch (RuntimeException exception) {
            try {
                topology.close();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    AppInstance appA() {
        return appA;
    }

    AppInstance appB() {
        return appB;
    }

    boolean relayPaused() {
        return relayPaused;
    }

    void relayOutbox() {
        appA.context().getBean(EventOutboxRelayService.class).relay();
    }

    void awaitUserRegistry(Long memberId, int expectedSessionCount) throws InterruptedException {
        SimpUserRegistry registry = appA.context().getBean(SimpUserRegistry.class);
        long deadline = System.nanoTime() + RELAY_TRANSITION_TIMEOUT.toNanos();
        do {
            SimpUser user = registry.getUser(memberId.toString());
            if (user != null && user.getSessions().size() >= expectedSessionCount) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        } while (System.nanoTime() < deadline);

        SimpUser user = registry.getUser(memberId.toString());
        int actualSessionCount = user == null ? 0 : user.getSessions().size();
        throw new AssertionError(
            "multi-server user registry가 제한 시간 안에 수렴하지 않았습니다: "
                + "memberId=%d, expected=%d, actual=%d"
                    .formatted(memberId, expectedSessionCount, actualSessionCount)
        );
    }

    void pauseRelay() throws InterruptedException {
        appA.relayChanges().discardPending();
        appB.relayChanges().discardPending();
        rabbitmq.getDockerClient().pauseContainerCmd(rabbitmq.getContainerId()).exec();
        relayPaused = true;
        requireRelayState(appA, false);
        requireRelayState(appB, false);
    }

    void resumeRelay() throws InterruptedException {
        appA.relayChanges().discardPending();
        appB.relayChanges().discardPending();
        rabbitmq.getDockerClient().unpauseContainerCmd(rabbitmq.getContainerId()).exec();
        relayPaused = false;
        requireRelayState(appA, true);
        requireRelayState(appB, true);
    }

    @Override
    public void close() {
        try {
            if (relayPaused && rabbitmq.isRunning()) {
                rabbitmq.getDockerClient().unpauseContainerCmd(rabbitmq.getContainerId()).exec();
                relayPaused = false;
            }
        } finally {
            try {
                closeContext(appB);
            } finally {
                try {
                    closeContext(appA);
                } finally {
                    try {
                        if (rabbitmq.isRunning()) {
                            rabbitmq.stop();
                        }
                    } finally {
                        if (postgis.isRunning()) {
                            postgis.stop();
                        }
                    }
                }
            }
        }
    }

    private AppInstance startApp(String applicationName, boolean outboxRelayEnabled) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
            UmcProductApplication.class,
            CommunityThreadTwoInstanceTestConfig.class
        )
            .profiles("test")
            .registerShutdownHook(false)
            .run(appArguments(applicationName, outboxRelayEnabled));
        WebServerApplicationContext webContext = (WebServerApplicationContext) context;
        return new AppInstance(
            context,
            webContext.getWebServer().getPort(),
            context.getBean(RelayAvailabilityProbe.class)
        );
    }

    private Map<String, Object> appProperties(String applicationName, boolean outboxRelayEnabled) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.application.name", applicationName);
        properties.put("spring.main.allow-bean-definition-overriding", "true");
        properties.put("server.port", "0");
        properties.put("management.server.port", "0");
        properties.put("spring.datasource.url", postgis.getJdbcUrl());
        properties.put("spring.datasource.username", postgis.getUsername());
        properties.put("spring.datasource.password", postgis.getPassword());
        properties.put("spring.flyway.out-of-order", "true");
        properties.put("jwt.access-token-secret", JWT_ACCESS_TOKEN_SECRET);
        properties.put("jwt.refresh-token-secret", JWT_REFRESH_TOKEN_SECRET);
        properties.put("jwt.oauth-verification-token-secret", JWT_OAUTH_VERIFICATION_TOKEN_SECRET);
        properties.put("jwt.email-verification-token-secret", JWT_EMAIL_VERIFICATION_TOKEN_SECRET);
        properties.put("jwt.sso-login-token-secret", JWT_SSO_LOGIN_TOKEN_SECRET);
        properties.put("storage.provider", "relay-e2e");
        properties.put("app.fcm.enabled", "false");
        properties.put("app.event-outbox.relay-enabled", Boolean.toString(outboxRelayEnabled));
        properties.put("app.event-outbox.poll-interval-ms", "3600000");
        properties.put("app.event-outbox.max-attempts", "2");
        properties.put("app.websocket.broker.mode", "relay");
        properties.put("app.websocket.broker.relay.host", rabbitmq.getHost());
        properties.put("app.websocket.broker.relay.port", rabbitmq.getMappedPort(STOMP_PORT).toString());
        properties.put("app.websocket.broker.relay.tls-enabled", "false");
        properties.put("app.websocket.broker.relay.virtual-host", "/");
        properties.put("app.websocket.broker.relay.system-login", BROKER_USER);
        properties.put("app.websocket.broker.relay.system-password", BROKER_PASSWORD);
        properties.put("app.websocket.broker.relay.client-login", BROKER_USER);
        properties.put("app.websocket.broker.relay.client-password", BROKER_PASSWORD);
        properties.put("app.websocket.broker.relay.system-heartbeat-send-interval", "PT0.5S");
        properties.put("app.websocket.broker.relay.system-heartbeat-receive-interval", "PT0.5S");
        properties.put("app.websocket.broker.relay.startup-timeout", "PT20S");
        return properties;
    }

    private String[] appArguments(String applicationName, boolean outboxRelayEnabled) {
        return appProperties(applicationName, outboxRelayEnabled).entrySet().stream()
            .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
            .toArray(String[]::new);
    }

    private void requireInitiallyAvailable(AppInstance app) {
        StompBrokerRelayMonitor monitor = app.context().getBean(StompBrokerRelayMonitor.class);
        try {
            if (!monitor.awaitAvailable(INITIAL_RELAY_TIMEOUT)) {
                throw new IllegalStateException("STOMP relay가 초기 연결되지 않았습니다: " + app.port());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("STOMP relay 초기 연결 대기가 중단됐습니다.", exception);
        }
    }

    private void requireRelayState(AppInstance app, boolean expected) throws InterruptedException {
        if (!app.relayChanges().await(expected, RELAY_TRANSITION_TIMEOUT)) {
            throw new AssertionError("STOMP relay 상태 전이를 받지 못했습니다: port=" + app.port()
                + ", expected=" + expected);
        }
    }

    private static void closeContext(AppInstance app) {
        if (app != null) {
            app.context().close();
        }
    }

    private static DockerImageName postgisImage() {
        String configured = System.getProperty(POSTGIS_IMAGE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(POSTGIS_IMAGE_ENV);
        }
        if (configured != null && !configured.isBlank()) {
            return postgresCompatible(configured.trim());
        }
        if ("aarch64".equals(System.getProperty("os.arch")) || "arm64".equals(System.getProperty("os.arch"))) {
            String image = new ImageFromDockerfile(ARM64_POSTGIS_IMAGE, false)
                .withDockerfile(ARM64_POSTGIS_DOCKERFILE)
                .get();
            return postgresCompatible(image);
        }
        return postgresCompatible(POSTGIS_IMAGE);
    }

    private static DockerImageName postgresCompatible(String image) {
        return DockerImageName.parse(image).asCompatibleSubstituteFor("postgres");
    }

    record AppInstance(
        ConfigurableApplicationContext context,
        int port,
        RelayAvailabilityProbe relayChanges
    ) {
    }
}
