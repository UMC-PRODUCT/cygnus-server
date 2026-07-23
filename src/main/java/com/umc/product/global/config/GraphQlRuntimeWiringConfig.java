package com.umc.product.global.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final GraphQLScalarType LOCAL_DATE_SCALAR = GraphQLScalarType.newScalar()
        .name("LocalDate")
        .description("ISO-8601 calendar date without a time zone")
        .coercing(new LocalDateCoercing())
        .build();

    private static final GraphQLScalarType LOCAL_DATE_TIME_SCALAR = GraphQLScalarType.newScalar()
        .name("LocalDateTime")
        .description("ISO-8601 local date-time without a time zone")
        .coercing(new LocalDateTimeCoercing())
        .build();

    @Bean
    public RuntimeWiringConfigurer graphQlRuntimeWiringConfigurer() {
        return this::configure;
    }

    public void configure(graphql.schema.idl.RuntimeWiring.Builder builder) {
        builder.scalar(LONG_SCALAR)
            .scalar(INSTANT_SCALAR)
            .scalar(LOCAL_DATE_SCALAR)
            .scalar(LOCAL_DATE_TIME_SCALAR);
    }

    private abstract static class IsoTemporalCoercing<T> implements Coercing<T, String> {

        private final Class<T> temporalType;
        private final String scalarName;

        private IsoTemporalCoercing(Class<T> temporalType, String scalarName) {
            this.temporalType = temporalType;
            this.scalarName = scalarName;
        }

        @Override
        public String serialize(Object dataFetcherResult, GraphQLContext graphQlContext, Locale locale)
            throws CoercingSerializeException {
            if (temporalType.isInstance(dataFetcherResult)) {
                return format(temporalType.cast(dataFetcherResult));
            }
            throw new CoercingSerializeException(scalarName + " scalar requires " + temporalType.getName());
        }

        @Override
        public T parseValue(Object input, GraphQLContext graphQlContext, Locale locale)
            throws CoercingParseValueException {
            if (!(input instanceof String value)) {
                throw new CoercingParseValueException(scalarName + " scalar requires an ISO-8601 string");
            }
            try {
                return parse(value);
            } catch (DateTimeParseException exception) {
                throw new CoercingParseValueException(scalarName + " scalar cannot parse value", exception);
            }
        }

        @Override
        public T parseLiteral(
            Value<?> input,
            CoercedVariables variables,
            GraphQLContext graphQlContext,
            Locale locale
        ) throws CoercingParseLiteralException {
            if (!(input instanceof StringValue value)) {
                throw new CoercingParseLiteralException(scalarName + " scalar requires an ISO-8601 string literal");
            }
            try {
                return parse(value.getValue());
            } catch (DateTimeParseException exception) {
                throw new CoercingParseLiteralException(scalarName + " scalar cannot parse literal", exception);
            }
        }

        @Override
        public Value<?> valueToLiteral(Object input, GraphQLContext graphQlContext, Locale locale) {
            if (temporalType.isInstance(input)) {
                return new StringValue(format(temporalType.cast(input)));
            }
            throw new CoercingSerializeException(scalarName + " scalar requires " + temporalType.getName());
        }

        protected abstract T parse(String value);

        protected abstract String format(T value);
    }

    private static class LocalDateCoercing extends IsoTemporalCoercing<LocalDate> {

        private LocalDateCoercing() {
            super(LocalDate.class, "LocalDate");
        }

        @Override
        protected LocalDate parse(String value) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }

        @Override
        protected String format(LocalDate value) {
            return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    private static class LocalDateTimeCoercing extends IsoTemporalCoercing<LocalDateTime> {

        private LocalDateTimeCoercing() {
            super(LocalDateTime.class, "LocalDateTime");
        }

        @Override
        protected LocalDateTime parse(String value) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        protected String format(LocalDateTime value) {
            return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private static class InstantCoercing implements Coercing<Instant, String> {

        @Override
        public String serialize(Object dataFetcherResult, GraphQLContext graphQlContext, Locale locale)
            throws CoercingSerializeException {
            if (dataFetcherResult instanceof Instant instant) {
                return instant.toString();
            }
            throw new CoercingSerializeException("Instant scalar requires java.time.Instant");
        }

        @Override
        public Instant parseValue(Object input, GraphQLContext graphQlContext, Locale locale)
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
            GraphQLContext graphQlContext,
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
        public Value<?> valueToLiteral(Object input, GraphQLContext graphQlContext, Locale locale) {
            if (input instanceof Instant instant) {
                return new StringValue(instant.toString());
            }
            throw new CoercingSerializeException("Instant scalar requires java.time.Instant");
        }
    }

    private static class LongCoercing implements Coercing<Long, Long> {

        @Override
        public Long serialize(Object dataFetcherResult, GraphQLContext graphQlContext, Locale locale)
            throws CoercingSerializeException {
            try {
                return toLong(dataFetcherResult);
            } catch (IllegalArgumentException ex) {
                throw new CoercingSerializeException("Long scalar cannot serialize value: " + dataFetcherResult, ex);
            }
        }

        @Override
        public Long parseValue(Object input, GraphQLContext graphQlContext, Locale locale)
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
            GraphQLContext graphQlContext,
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
        public Value<?> valueToLiteral(Object input, GraphQLContext graphQlContext, Locale locale) {
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
                return exactLong(bigDecimal.toBigIntegerExact());
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
