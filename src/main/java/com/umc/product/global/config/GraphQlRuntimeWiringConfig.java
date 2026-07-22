package com.umc.product.global.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

@Configuration
public class GraphQlRuntimeWiringConfig {

    private static final GraphQLScalarType LONG_SCALAR = GraphQLScalarType.newScalar()
        .name("Long")
        .description("64-bit signed integer")
        .coercing(new LongCoercing())
        .build();

    private static final GraphQLScalarType INSTANT_SCALAR = GraphQLScalarType.newScalar()
        .name("Instant")
        .description("ISO-8601 UTC instant")
        .coercing(new InstantCoercing())
        .build();

    @Bean
    public RuntimeWiringConfigurer graphQlRuntimeWiringConfigurer() {
        return this::configure;
    }

    public void configure(graphql.schema.idl.RuntimeWiring.Builder builder) {
        builder.scalar(LONG_SCALAR).scalar(INSTANT_SCALAR);
    }

    private static class InstantCoercing implements Coercing<Instant, String> {

        @Override
        public String serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale)
            throws CoercingSerializeException {
            if (dataFetcherResult instanceof Instant instant) {
                return instant.toString();
            }
            throw new CoercingSerializeException("Instant scalar requires java.time.Instant");
        }

        @Override
        public Instant parseValue(Object input, GraphQLContext graphQLContext, Locale locale)
            throws CoercingParseValueException {
            if (!(input instanceof String value)) {
                throw new CoercingParseValueException("Instant scalar requires an ISO-8601 string");
            }
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException exception) {
                throw new CoercingParseValueException("Instant scalar cannot parse value", exception);
            }
        }

        @Override
        public Instant parseLiteral(
            Value<?> input,
            CoercedVariables variables,
            GraphQLContext graphQLContext,
            Locale locale
        ) throws CoercingParseLiteralException {
            if (!(input instanceof StringValue value)) {
                throw new CoercingParseLiteralException("Instant scalar requires an ISO-8601 string literal");
            }
            try {
                return Instant.parse(value.getValue());
            } catch (DateTimeParseException exception) {
                throw new CoercingParseLiteralException("Instant scalar cannot parse literal", exception);
            }
        }

        @Override
        public Value<?> valueToLiteral(Object input, GraphQLContext graphQLContext, Locale locale) {
            if (input instanceof Instant instant) {
                return new StringValue(instant.toString());
            }
            throw new CoercingSerializeException("Instant scalar requires java.time.Instant");
        }
    }

    private static class LongCoercing implements Coercing<Long, Long> {

        @Override
        public Long serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale)
            throws CoercingSerializeException {
            try {
                return toLong(dataFetcherResult);
            } catch (IllegalArgumentException ex) {
                throw new CoercingSerializeException("Long scalar cannot serialize value: " + dataFetcherResult, ex);
            }
        }

        @Override
        public Long parseValue(Object input, GraphQLContext graphQLContext, Locale locale)
            throws CoercingParseValueException {
            try {
                return toLong(input);
            } catch (IllegalArgumentException ex) {
                throw new CoercingParseValueException("Long scalar cannot parse value: " + input, ex);
            }
        }

        @Override
        public Long parseLiteral(
            Value<?> input,
            CoercedVariables variables,
            GraphQLContext graphQLContext,
            Locale locale
        ) throws CoercingParseLiteralException {
            try {
                if (input instanceof IntValue intValue) {
                    return toLong(intValue.getValue());
                }
                if (input instanceof StringValue stringValue) {
                    return toLong(stringValue.getValue());
                }
                throw new IllegalArgumentException("Unsupported literal type");
            } catch (IllegalArgumentException ex) {
                throw new CoercingParseLiteralException("Long scalar cannot parse literal: " + input, ex);
            }
        }

        @Override
        public Value<?> valueToLiteral(Object input, GraphQLContext graphQLContext, Locale locale) {
            return new IntValue(BigInteger.valueOf(toLong(input)));
        }

        private static Long toLong(Object value) {
            if (value instanceof Long longValue) {
                return longValue;
            }
            if (value instanceof Integer
                || value instanceof Short
                || value instanceof Byte) {
                return ((Number)value).longValue();
            }
            if (value instanceof BigInteger bigInteger) {
                return exactLong(bigInteger);
            }
            if (value instanceof BigDecimal bigDecimal) {
                try {
                    return exactLong(bigDecimal.toBigIntegerExact());
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException("Expected an integer decimal value", ex);
                }
            }
            if (value instanceof String stringValue) {
                return Long.parseLong(stringValue);
            }
            throw new IllegalArgumentException("Expected integer-compatible value");
        }

        private static Long exactLong(BigInteger bigInteger) {
            try {
                return bigInteger.longValueExact();
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("Long value is out of range", ex);
            }
        }
    }
}
