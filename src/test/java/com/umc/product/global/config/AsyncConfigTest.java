package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AsyncConfigTest {

    @Test
    @DisplayName("비동기 예외 handler는 method와 예외를 안전하게 기록한다")
    void async_exception_handler() throws NoSuchMethodException {
        AsyncConfig config = new AsyncConfig();
        Method method = AsyncConfigTest.class.getDeclaredMethod("sampleMethod", String.class);

        assertThatCode(() -> config.getAsyncUncaughtExceptionHandler().handleUncaughtException(
            new IllegalStateException("실패"),
            method,
            "parameter"
        )).doesNotThrowAnyException();
    }

    @SuppressWarnings("unused")
    private void sampleMethod(String value) {
    }
}
