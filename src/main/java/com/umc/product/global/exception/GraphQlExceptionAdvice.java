package com.umc.product.global.exception;

import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.ApiErrorResponseFactory;
import com.umc.product.global.response.code.BaseCode;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GraphQlExceptionAdvice {

    @GraphQlExceptionHandler(AccessDeniedException.class)
    public GraphQLError handleAccessDeniedException(AccessDeniedException exception, DataFetchingEnvironment env) {
        log.warn("[GRAPHQL ACCESS DENIED] path={}, message={}", env.getExecutionStepInfo().getPath(),
            exception.getMessage());
        return buildError(env, ErrorType.FORBIDDEN, CommonErrorCode.FORBIDDEN, exception.getMessage());
    }

    @GraphQlExceptionHandler(BusinessException.class)
    public GraphQLError handleBusinessException(BusinessException exception, DataFetchingEnvironment env) {
        log.warn("[GRAPHQL BUSINESS EXCEPTION] path={}, domain={}, code={}, message={}",
            env.getExecutionStepInfo().getPath(), exception.getDomain(), exception.getBaseCode().getCode(),
            exception.getMessage());
        return buildError(
            env,
            errorType(exception.getBaseCode()),
            exception.getBaseCode(),
            ApiErrorResponseFactory.resolveMessage(exception.getBaseCode(), exception.getMessage())
        );
    }

    @GraphQlExceptionHandler({
        IllegalArgumentException.class,
        BindException.class,
        ConstraintViolationException.class,
        DateTimeParseException.class
    })
    public GraphQLError handleBadRequestException(Exception exception, DataFetchingEnvironment env) {
        log.warn("[GRAPHQL BAD REQUEST] path={}, message={}", env.getExecutionStepInfo().getPath(),
            exception.getMessage());
        return buildError(env, ErrorType.BAD_REQUEST, CommonErrorCode.BAD_REQUEST, exception.getMessage());
    }

    @GraphQlExceptionHandler(Exception.class)
    public GraphQLError handleUnhandledException(Exception exception, DataFetchingEnvironment env) {
        log.error("[GRAPHQL UNHANDLED EXCEPTION] path={}, message={}", env.getExecutionStepInfo().getPath(),
            exception.getMessage(), exception);
        return buildError(
            env,
            ErrorType.INTERNAL_ERROR,
            CommonErrorCode.INTERNAL_SERVER_ERROR,
            CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
    }

    private GraphQLError buildError(
        DataFetchingEnvironment env,
        ErrorType errorType,
        BaseCode code,
        String message
    ) {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("code", code.getCode());
        extensions.put("httpStatus", code.getHttpStatus().value());

        return GraphqlErrorBuilder.newError(env)
            .message(ApiErrorResponseFactory.resolveMessage(code, message))
            .errorType(errorType)
            .extensions(extensions)
            .build();
    }

    private ErrorType errorType(BaseCode code) {
        return switch (code.getHttpStatus()) {
            case BAD_REQUEST -> ErrorType.BAD_REQUEST;
            case UNAUTHORIZED -> ErrorType.UNAUTHORIZED;
            case FORBIDDEN -> ErrorType.FORBIDDEN;
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }
}
