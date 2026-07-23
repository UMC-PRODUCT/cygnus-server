package com.umc.product.term.adapter.in.graphql;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.stereotype.Component;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.term.config.TermConsentEnforcementProperties;
import com.umc.product.term.domain.exception.TermErrorCode;

import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TermConsentGraphQlInterceptor implements WebGraphQlInterceptor {

    private static final String METRIC_DOMAIN = "terms";
    private static final String METRIC_OPERATION = "reconsent_enforcement";

    private final TermConsentEnforcementProperties properties;
    private final CurrentMemberProvider currentMemberProvider;
    private final OperationalMetrics operationalMetrics;

    public TermConsentGraphQlInterceptor(
        TermConsentEnforcementProperties properties,
        CurrentMemberProvider currentMemberProvider,
        OperationalMetrics operationalMetrics
    ) {
        this.properties = properties;
        this.currentMemberProvider = currentMemberProvider;
        this.operationalMetrics = operationalMetrics;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        MemberPrincipal principal = currentMemberProvider.getNullableCurrentMember();
        if (!properties.enabled() || principal == null || principal.isRequiredTermsAgreed()) {
            return chain.next(request);
        }

        operationalMetrics.recordSecurityEvent(METRIC_DOMAIN, METRIC_OPERATION, "blocked_graphql");
        return Mono.just(reconsentRequiredResponse(request));
    }

    private WebGraphQlResponse reconsentRequiredResponse(WebGraphQlRequest request) {
        TermErrorCode errorCode = TermErrorCode.TERMS_RECONSENT_REQUIRED;
        ExecutionResult result = ExecutionResultImpl.newExecutionResult()
            .addError(GraphqlErrorBuilder.newError()
                .message(errorCode.getMessage())
                .errorType(ErrorType.ExecutionAborted)
                .extensions(Map.of(
                    "code", errorCode.getCode(),
                    "httpStatus", errorCode.getHttpStatus().value()
                ))
                .build())
            .build();
        return new WebGraphQlResponse(
            new DefaultExecutionGraphQlResponse(request.toExecutionInput(), result)
        );
    }
}
