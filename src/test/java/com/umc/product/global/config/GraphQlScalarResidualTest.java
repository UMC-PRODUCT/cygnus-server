package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.BooleanValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;

@DisplayName("GraphQL custom scalar coercing")
class GraphQlScalarResidualTest {

    private static final GraphQLContext CONTEXT = GraphQLContext.newContext().build();
    private static final Locale LOCALE = Locale.KOREA;

    @Test
    @DisplayName("Instant scalar는 ISO-8601 값의 serialize·variable·literal 왕복만 허용한다")
    void Instant_scalar를_검증한다() {
        Coercing<Object, Object> sut = coercing("Instant");
        Instant instant = Instant.parse("2026-07-22T00:00:00Z");

        assertThat(sut.serialize(instant, CONTEXT, LOCALE)).isEqualTo("2026-07-22T00:00:00Z");
        assertThat(sut.parseValue("2026-07-22T00:00:00Z", CONTEXT, LOCALE)).isEqualTo(instant);
        assertThat(sut.parseLiteral(
            new StringValue("2026-07-22T00:00:00Z"), CoercedVariables.emptyVariables(), CONTEXT, LOCALE))
            .isEqualTo(instant);
        assertThat(((StringValue)sut.valueToLiteral(instant, CONTEXT, LOCALE)).getValue())
            .isEqualTo(instant.toString());

        assertThatThrownBy(() -> sut.serialize("not-instant", CONTEXT, LOCALE))
            .isInstanceOf(CoercingSerializeException.class);
        assertThatThrownBy(() -> sut.parseValue(1L, CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> sut.parseValue("invalid", CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> sut.parseLiteral(
            new IntValue(BigInteger.ONE), CoercedVariables.emptyVariables(), CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseLiteralException.class);
        assertThatThrownBy(() -> sut.parseLiteral(
            new StringValue("invalid"), CoercedVariables.emptyVariables(), CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseLiteralException.class);
        assertThatThrownBy(() -> sut.valueToLiteral("invalid", CONTEXT, LOCALE))
            .isInstanceOf(CoercingSerializeException.class);
    }

    @Test
    @DisplayName("Long scalar는 정수 호환 타입을 exact long으로 변환한다")
    void Long_scalar의_호환_타입을_검증한다() {
        Coercing<Object, Object> sut = coercing("Long");

        assertThat(sut.serialize(1L, CONTEXT, LOCALE)).isEqualTo(1L);
        assertThat(sut.serialize(2, CONTEXT, LOCALE)).isEqualTo(2L);
        assertThat(sut.serialize((short)3, CONTEXT, LOCALE)).isEqualTo(3L);
        assertThat(sut.serialize((byte)4, CONTEXT, LOCALE)).isEqualTo(4L);
        assertThat(sut.serialize(BigInteger.valueOf(5), CONTEXT, LOCALE)).isEqualTo(5L);
        assertThat(sut.serialize(new BigDecimal("6"), CONTEXT, LOCALE)).isEqualTo(6L);
        assertThat(sut.parseValue("7", CONTEXT, LOCALE)).isEqualTo(7L);
        assertThat(sut.parseLiteral(
            new IntValue(BigInteger.valueOf(8)), CoercedVariables.emptyVariables(), CONTEXT, LOCALE)).isEqualTo(8L);
        assertThat(sut.parseLiteral(
            new StringValue("9"), CoercedVariables.emptyVariables(), CONTEXT, LOCALE)).isEqualTo(9L);
        assertThat(((IntValue)sut.valueToLiteral(10, CONTEXT, LOCALE)).getValue()).isEqualTo(BigInteger.TEN);
    }

    @Test
    @DisplayName("Long scalar는 소수·범위 초과·미지원 타입을 단계별 coercing 예외로 변환한다")
    void Long_scalar의_잘못된_값을_거부한다() {
        Coercing<Object, Object> sut = coercing("Long");

        assertThatThrownBy(() -> sut.serialize(new Object(), CONTEXT, LOCALE))
            .isInstanceOf(CoercingSerializeException.class);
        assertThatThrownBy(() -> sut.parseValue(new BigDecimal("1.5"), CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> sut.parseValue(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> sut.parseValue("not-long", CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> sut.parseLiteral(
            BooleanValue.newBooleanValue(true).build(), CoercedVariables.emptyVariables(), CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseLiteralException.class);
        assertThatThrownBy(() -> sut.parseLiteral(
            new StringValue("not-long"), CoercedVariables.emptyVariables(), CONTEXT, LOCALE))
            .isInstanceOf(CoercingParseLiteralException.class);
    }

    @SuppressWarnings("unchecked")
    private Coercing<Object, Object> coercing(String scalarName) {
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        new GraphQlRuntimeWiringConfig().configure(builder);
        GraphQLScalarType scalar = builder.build().getScalars().get(scalarName);
        return (Coercing<Object, Object>)scalar.getCoercing();
    }
}
