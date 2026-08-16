package com.umc.product.support.concurrency;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgreSqlTransactionRace implements AutoCloseable {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public PostgreSqlTransactionRace(
        PlatformTransactionManager transactionManager,
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public <T> TransactionCall<T> submit(Supplier<T> action) {
        CompletableFuture<Integer> backendPid = new CompletableFuture<>();
        Future<T> future = executor.submit(() -> transactionTemplate.execute(status -> {
            backendPid.complete(jdbcTemplate.queryForObject(
                "SELECT pg_backend_pid()",
                Integer.class
            ));
            return action.get();
        }));
        return new TransactionCall<>(future, backendPid);
    }

    public void awaitPostgreSqlLockWait(TransactionCall<?> call) throws Exception {
        int backendPid = call.backendPid().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Integer blockerCount = jdbcTemplate.queryForObject(
                "SELECT cardinality(pg_blocking_pids(?))",
                Integer.class,
                backendPid
            );
            if (blockerCount != null && blockerCount > 0) {
                return;
            }
            if (call.future().isDone()) {
                throw new AssertionError(
                    "transaction finished before PostgreSQL lock wait was observed"
                );
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("PostgreSQL lock wait was not observed before timeout");
    }

    public void await(CountDownLatch latch, String seam) {
        try {
            if (!latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                throw new AssertionError(seam + " timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(seam + " interrupted", exception);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public record TransactionCall<T>(
        Future<T> future,
        CompletableFuture<Integer> backendPid
    ) {
    }
}
