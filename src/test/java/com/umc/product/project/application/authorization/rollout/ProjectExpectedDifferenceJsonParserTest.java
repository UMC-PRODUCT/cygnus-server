package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProjectExpectedDifferenceJsonParserTest {

    private final ProjectExpectedDifferenceJsonParser parser = new ProjectExpectedDifferenceJsonParser();

    @Test
    @DisplayName("fixed classpath expected-difference matrix를 strict contract로 읽는다")
    void parsesTrackedResource() {
        assertThat(parser.parseClasspath().entries()).hasSize(58);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDocuments")
    @DisplayName("unknown duplicate null coercion wildcard action을 거부한다")
    void rejectsInvalidDocuments(String name, byte[] json) {
        assertThatThrownBy(() -> parser.parse(json))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("EXPECTED_DIFFERENCE_");
    }

    private static Stream<Arguments> invalidDocuments() throws IOException {
        String valid = resource();
        return Stream.of(
            Arguments.of("unknown-field", bytes(valid.replaceFirst(
                "\\{\\\"id\\\":", "{\"unknown\":true,\"id\":"))),
            Arguments.of("duplicate-key", bytes(valid.replaceFirst(
                "\\{", "{\"schemaVersion\":\"1.0\","))),
            Arguments.of("null", bytes(valid.replaceFirst(
                "\\\"owner\\\":\\\"project-authorization\\\"", "\"owner\":null"))),
            Arguments.of("boolean-coercion", bytes(valid.replace(
                "\"classificationOnly\": true", "\"classificationOnly\": \"true\""))),
            Arguments.of("wildcard-action", bytes(valid.replaceFirst(
                "\\\"action\\\":\\\"project:update-info\\\"",
                "\"action\":\"project:*\""))),
            Arguments.of("unknown-action", bytes(valid.replaceFirst(
                "\\\"action\\\":\\\"project:update-info\\\"",
                "\"action\":\"project:unknown\""))),
            Arguments.of("json-null-outcome", bytes(valid.replaceFirst(
                "\\\"outcomes\\\":\\[\\]", "\"outcomes\":null")))
        );
    }

    private static String resource() throws IOException {
        try (var input = ProjectExpectedDifferenceJsonParserTest.class.getClassLoader()
            .getResourceAsStream(ProjectExpectedDifferenceJsonParser.RESOURCE)) {
            if (input == null) {
                throw new IOException("expected-difference resource가 없습니다.");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
