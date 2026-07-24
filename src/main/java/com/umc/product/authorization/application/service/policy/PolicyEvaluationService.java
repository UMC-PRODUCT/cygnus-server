package com.umc.product.authorization.application.service.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode;
import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;

@Service
public class PolicyEvaluationService implements EvaluatePolicyUseCase {

    private final PolicyConditionEvaluator conditionEvaluator = new PolicyConditionEvaluator();
    private final PolicyOutcomeMerger outcomeMerger = new PolicyOutcomeMerger();

    @Override
    public PolicyEvaluationResult evaluate(PolicyEvaluationRequest request) {
        CompiledPolicyBundle bundle = request.bundle();
        Optional<ActionSchema> action = bundle.domainSchema().action(request.actionId());
        if (action.isEmpty()) {
            return failure(
                    request,
                    PolicyEvaluationFailureCode.ACTION_NOT_REGISTERED,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        Optional<String> missingRequired = action.orElseThrow().requiredAttributes().stream()
                .sorted()
                .filter(attribute -> !request.attributes().contains(attribute))
                .findFirst();
        if (missingRequired.isPresent()) {
            return failure(
                    request,
                    PolicyEvaluationFailureCode.REQUIRED_ATTRIBUTE_MISSING,
                    Optional.empty(),
                    missingRequired,
                    Optional.empty());
        }

        Optional<PolicyEvaluationFailure> invalidAttribute = validateAttributes(request);
        if (invalidAttribute.isPresent()) {
            return invalidAttribute.orElseThrow();
        }

        List<CompiledPolicyStatement> matchedAllow = new ArrayList<>();
        List<CompiledPolicyStatement> matchedDeny = new ArrayList<>();
        bundle.statementsForAction(request.actionId())
                .forEach(statement -> match(statement, request.attributes(), matchedAllow, matchedDeny));
        matchedAllow.sort(Comparator.comparing(CompiledPolicyStatement::id));
        matchedDeny.sort(Comparator.comparing(CompiledPolicyStatement::id));

        List<String> allowIds = matchedAllow.stream().map(CompiledPolicyStatement::id).toList();
        List<String> denyIds = matchedDeny.stream().map(CompiledPolicyStatement::id).toList();
        if (!matchedDeny.isEmpty()) {
            return decision(request, PolicyEffect.DENY, allowIds, denyIds, List.of());
        }
        if (matchedAllow.isEmpty()) {
            return decision(request, bundle.defaultEffect(), List.of(), List.of(), List.of());
        }

        PolicyOutcomeMergeResult mergeResult =
                outcomeMerger.merge(matchedAllow, request.attributes(), bundle.domainSchema());
        if (mergeResult instanceof PolicyOutcomeMergeResult.Failure mergeFailure) {
            return failure(
                    request,
                    mergeFailure.code(),
                    mergeFailure.statementId(),
                    mergeFailure.attributeName(),
                    mergeFailure.outcomeKey());
        }
        PolicyOutcomeMergeResult.Success success = (PolicyOutcomeMergeResult.Success) mergeResult;
        return decision(request, PolicyEffect.ALLOW, allowIds, List.of(), success.outcomes());
    }

    private Optional<PolicyEvaluationFailure> validateAttributes(PolicyEvaluationRequest request) {
        for (PolicyAttributeSet.PolicyAttribute attribute : request.attributes().entries()) {
            Optional<AttributeSchema> schema = request.bundle().domainSchema().attribute(attribute.name());
            if (schema.isEmpty()) {
                return Optional.of(failure(
                        request,
                        PolicyEvaluationFailureCode.ATTRIBUTE_NOT_REGISTERED,
                        Optional.empty(),
                        Optional.of(attribute.name()),
                        Optional.empty()));
            }
            AttributeSchema registered = schema.orElseThrow();
            if (registered.type() != attribute.declaredType() || registered.type() != attribute.value().type()) {
                return Optional.of(failure(
                        request,
                        PolicyEvaluationFailureCode.ATTRIBUTE_TYPE_MISMATCH,
                        Optional.empty(),
                        Optional.of(attribute.name()),
                        Optional.empty()));
            }
            if (!containsKnownEnumSymbols(registered, attribute.value())) {
                return Optional.of(failure(
                        request,
                        PolicyEvaluationFailureCode.ATTRIBUTE_ENUM_SYMBOL_UNKNOWN,
                        Optional.empty(),
                        Optional.of(attribute.name()),
                        Optional.empty()));
            }
        }
        return Optional.empty();
    }

    private boolean containsKnownEnumSymbols(AttributeSchema schema, PolicyValue value) {
        Set<String> symbols;
        if (value instanceof PolicyValue.EnumValue enumValue) {
            symbols = Set.of(enumValue.value());
        } else if (value instanceof PolicyValue.EnumSetValue enumSetValue) {
            symbols = enumSetValue.value();
        } else {
            return true;
        }
        return schema.enumSymbols().containsAll(symbols);
    }

    private void match(
            CompiledPolicyStatement statement,
            PolicyAttributeSet attributes,
            List<CompiledPolicyStatement> matchedAllow,
            List<CompiledPolicyStatement> matchedDeny) {
        if (!conditionEvaluator.evaluate(statement.condition(), attributes)) {
            return;
        }
        if (statement.effect() == PolicyEffect.ALLOW) {
            matchedAllow.add(statement);
        } else {
            matchedDeny.add(statement);
        }
    }

    private PolicyDecision decision(
            PolicyEvaluationRequest request,
            PolicyEffect effect,
            List<String> allowIds,
            List<String> denyIds,
            List<PolicyResolvedOutcome> outcomes) {
        CompiledPolicyBundle bundle = request.bundle();
        return new PolicyDecision(
                effect,
                allowIds,
                denyIds,
                outcomes,
                request.evaluatedAt(),
                bundle.schemaVersion(),
                bundle.contextSchemaVersion(),
                bundle.policyVersion(),
                bundle.policyFingerprint());
    }

    private PolicyEvaluationFailure failure(
            PolicyEvaluationRequest request,
            PolicyEvaluationFailureCode code,
            Optional<String> statementId,
            Optional<String> attributeName,
            Optional<String> outcomeKey) {
        CompiledPolicyBundle bundle = request.bundle();
        return new PolicyEvaluationFailure(
                code,
                statementId,
                attributeName,
                outcomeKey,
                request.evaluatedAt(),
                bundle.schemaVersion(),
                bundle.contextSchemaVersion(),
                bundle.policyVersion(),
                bundle.policyFingerprint());
    }
}
