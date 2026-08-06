package com.umc.product.recruiting.adapter.in.graphql;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.ratelimit.ApiRateLimitMetrics;
import com.umc.product.global.ratelimit.RateLimitBucketRegistry;
import com.umc.product.global.ratelimit.RateLimitPolicy;

import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RecruitingCredentialGraphQlRateLimitInterceptor implements WebGraphQlInterceptor {

    private static final Set<String> CREDENTIAL_FIELDS = Set.of(
        "recruitingApplicationByCredential",
        "updateAnonymousRecruitingApplication",
        "submitAnonymousRecruitingApplication",
        "cancelAnonymousRecruitingApplication"
    );
    private static final String POLICY_NAME = "recruiting-graphql-credential";
    private static final String ROUTE_NAME = "recruitingCredentialGraphQl";
    private static final RateLimitPolicy POLICY = new RateLimitPolicy(POLICY_NAME, 1, 5);

    private final RateLimitBucketRegistry bucketRegistry;
    private final ApiRateLimitMetrics metrics;

    public RecruitingCredentialGraphQlRateLimitInterceptor(
        RateLimitBucketRegistry bucketRegistry,
        ApiRateLimitMetrics metrics
    ) {
        this.bucketRegistry = bucketRegistry;
        this.metrics = metrics;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        int requestedOperations = countCredentialFields(request);
        if (requestedOperations == 0) {
            return chain.next(request);
        }

        String bucketKey = "graphql:recruiting-credential:ip:" + resolveClientIp(request.getRemoteAddress());
        Bucket bucket = bucketRegistry.get(bucketKey, POLICY);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(requestedOperations);
        if (probe.isConsumed()) {
            metrics.record("allowed", POLICY_NAME, "POST", ROUTE_NAME, "ANONYMOUS");
            return chain.next(request).map(response -> withRateLimitHeaders(response, probe));
        }

        metrics.record("blocked", POLICY_NAME, "POST", ROUTE_NAME, "ANONYMOUS");
        return Mono.just(rateLimitedResponse(request, probe));
    }

    private int countCredentialFields(WebGraphQlRequest request) {
        try {
            Document document = Parser.parse(request.getDocument());
            OperationDefinition operation = resolveOperation(document, request.getOperationName());
            if (operation == null) {
                return 0;
            }
            Map<String, FragmentDefinition> fragments = new HashMap<>();
            document.getDefinitionsOfType(FragmentDefinition.class)
                .forEach(fragment -> fragments.put(fragment.getName(), fragment));
            return countCredentialFields(operation.getSelectionSet(), fragments, new HashSet<>());
        } catch (InvalidSyntaxException ignored) {
            return 0;
        }
    }

    private OperationDefinition resolveOperation(Document document, String operationName) {
        List<OperationDefinition> operations = document.getDefinitionsOfType(OperationDefinition.class);
        if (operationName == null || operationName.isBlank()) {
            return operations.size() == 1 ? operations.getFirst() : null;
        }
        return operations.stream()
            .filter(operation -> operationName.equals(operation.getName()))
            .findFirst()
            .orElse(null);
    }

    private int countCredentialFields(
        SelectionSet selectionSet,
        Map<String, FragmentDefinition> fragments,
        Set<String> visitedFragments
    ) {
        if (selectionSet == null) {
            return 0;
        }
        int count = 0;
        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field field) {
                if (CREDENTIAL_FIELDS.contains(field.getName())) {
                    count++;
                }
            } else if (selection instanceof InlineFragment inlineFragment) {
                count += countCredentialFields(inlineFragment.getSelectionSet(), fragments, visitedFragments);
            } else if (selection instanceof FragmentSpread fragmentSpread
                && visitedFragments.add(fragmentSpread.getName())) {
                FragmentDefinition fragment = fragments.get(fragmentSpread.getName());
                if (fragment != null) {
                    count += countCredentialFields(fragment.getSelectionSet(), fragments, visitedFragments);
                }
            }
        }
        return count;
    }

    private WebGraphQlResponse rateLimitedResponse(WebGraphQlRequest request, ConsumptionProbe probe) {
        CommonErrorCode errorCode = CommonErrorCode.TOO_MANY_REQUESTS;
        ExecutionResult result = ExecutionResultImpl.newExecutionResult()
            .addError(GraphqlErrorBuilder.newError()
                .message(errorCode.getMessage())
                .errorType(ErrorType.ExecutionAborted)
                .extensions(Map.of(
                    "code", errorCode.getCode(),
                    "status", errorCode.getHttpStatus().value()
                ))
                .build())
            .build();
        WebGraphQlResponse response = new WebGraphQlResponse(
            new DefaultExecutionGraphQlResponse(request.toExecutionInput(), result)
        );
        response.getResponseHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(probe)));
        response.getResponseHeaders().set("X-RateLimit-Remaining", "0");
        response.getResponseHeaders().set("X-RateLimit-Limit", String.valueOf(POLICY.requestsPerMinute()));
        return response;
    }

    private WebGraphQlResponse withRateLimitHeaders(WebGraphQlResponse response, ConsumptionProbe probe) {
        response.getResponseHeaders().set("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.getResponseHeaders().set("X-RateLimit-Limit", String.valueOf(POLICY.requestsPerMinute()));
        return response;
    }

    private long retryAfterSeconds(ConsumptionProbe probe) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    }

    private String resolveClientIp(InetSocketAddress remoteAddress) {
        if (remoteAddress == null) {
            return "unknown";
        }
        return remoteAddress.getAddress() == null
            ? remoteAddress.getHostString()
            : remoteAddress.getAddress().getHostAddress();
    }
}
