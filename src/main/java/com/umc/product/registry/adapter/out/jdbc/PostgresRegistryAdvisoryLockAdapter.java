package com.umc.product.registry.adapter.out.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.umc.product.registry.application.port.out.RegistryAdvisoryLockPort;

import lombok.RequiredArgsConstructor;

@Component
@Profile("registry-backfill")
@RequiredArgsConstructor
public class PostgresRegistryAdvisoryLockAdapter implements RegistryAdvisoryLockPort {

    private static final String LOCK_SCOPE = "umc-product:registry-backfill:";

    private final DataSource dataSource;

    @Override
    public RegistryAdvisoryLock acquire(String registryName) {
        Connection connection = openConnection();
        try {
            if (!executeBoolean(connection,
                "SELECT pg_try_advisory_lock(hashtextextended(?, 0))",
                LOCK_SCOPE + registryName)) {
                connection.close();
                throw new IllegalStateException("registry backfill advisory lock이 이미 사용 중입니다.");
            }
            return new JdbcRegistryAdvisoryLock(connection, LOCK_SCOPE + registryName);
        } catch (SQLException | RuntimeException e) {
            closeAfterFailure(connection, e);
            throw new IllegalStateException("registry backfill advisory lock 획득에 실패했습니다.", e);
        }
    }

    private Connection openConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("registry advisory lock connection을 열지 못했습니다.", e);
        }
    }

    private static boolean executeBoolean(Connection connection, String sql, String lockName)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, lockName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("advisory lock 결과가 없습니다.");
                }
                return resultSet.getBoolean(1);
            }
        }
    }

    private static void closeAfterFailure(Connection connection, Exception failure) {
        try {
            connection.close();
        } catch (SQLException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static final class JdbcRegistryAdvisoryLock implements RegistryAdvisoryLock {

        private final Connection connection;
        private final String lockName;
        private boolean closed;

        private JdbcRegistryAdvisoryLock(Connection connection, String lockName) {
            this.connection = connection;
            this.lockName = lockName;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                executeBoolean(
                    connection,
                    "SELECT pg_advisory_unlock(hashtextextended(?, 0))",
                    lockName
                );
            } catch (SQLException e) {
                throw new IllegalStateException("registry advisory lock 해제에 실패했습니다.", e);
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new IllegalStateException("registry advisory lock connection 종료에 실패했습니다.", e);
                }
            }
        }
    }
}
