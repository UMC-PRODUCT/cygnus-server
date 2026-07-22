package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;

@DisplayName("CustomHighlightConverter")
class CustomHighlightConverterTest {

    @Test
    @DisplayName("각 log level을 고정된 ANSI 색상으로 변환하고 미지원 level은 기본색을 사용한다")
    void log_level_색상을_변환한다() {
        ExposedConverter sut = new ExposedConverter();

        assertThat(sut.color(Level.ERROR)).isEqualTo(ANSIConstants.RED_FG);
        assertThat(sut.color(Level.WARN)).isEqualTo(ANSIConstants.YELLOW_FG);
        assertThat(sut.color(Level.INFO)).isEqualTo(ANSIConstants.BLUE_FG);
        assertThat(sut.color(Level.DEBUG)).isEqualTo(ANSIConstants.CYAN_FG);
        assertThat(sut.color(Level.TRACE)).isEqualTo(ANSIConstants.MAGENTA_FG);
        assertThat(sut.color(Level.OFF)).isEqualTo(ANSIConstants.DEFAULT_FG);
    }

    private static final class ExposedConverter extends CustomHighlightConverter {
        String color(Level level) {
            ILoggingEvent event = mock(ILoggingEvent.class);
            given(event.getLevel()).willReturn(level);
            return getForegroundColorCode(event);
        }
    }
}
