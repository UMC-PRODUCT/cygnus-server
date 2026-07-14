package com.umc.product.authorization.application.service.policy;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.CompilePolicyBundleUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyCombiningAlgorithm;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyOperator;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

@Service
public class PolicySemanticCompiler implements CompilePolicyBundleUseCase {

    static final int MAX_STATEMENTS = 500;
    static final int MAX_ACTIONS_PER_STATEMENT = 32;
    static final int MAX_CONDITION_DEPTH = 16;
    static final int MAX_CONDITION_CHILDREN = 64;
    static final int MAX_CONDITION_NODES = 128;
    static final int MAX_SET_VALUES = 256;
    static final int MAX_IDENTIFIER_LENGTH = 128;
    static final int MAX_STRING_LENGTH = 1024;

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");
    private static final Pattern SEMVER = Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)");

    private final PolicyJsonParser parser;
    private final PolicyAstCanonicalizer canonicalizer;
    private final PolicyFingerprintCalculator fingerprintCalculator;

    public PolicySemanticCompiler() {
        this.parser = new PolicyJsonParser();
        this.canonicalizer = new PolicyAstCanonicalizer();
        this.fingerprintCalculator = new PolicyFingerprintCalculator();
    }

    @Override
    public CompiledPolicyBundle compile(PolicyBundleCompilationRequest request) {
        byte[] bundleJson = request.bundleJson();
        Map<String, byte[]> moduleJsonByFilename = request.moduleJsonByFilename();
        enforceBundleSize(bundleJson, moduleJsonByFilename);

        PolicySchema10Wire.Bundle bundle = parser.parseBundle(bundleJson);
        PolicyDomainSchema domainSchema = request.domainSchema();
        EnvelopeValues envelope = compileEnvelope(bundle, domainSchema);
        Map<String, PolicySchema10Wire.ModuleReference> references = validateReferences(bundle.modules());
        validateProvidedModules(references, moduleJsonByFilename);

        int statementCount = 0;
        Set<String> statementIds = new HashSet<>();
        List<CompiledPolicyModule> modules = new ArrayList<>();
        for (PolicySchema10Wire.ModuleReference reference : bundle.modules()) {
            PolicySchema10Wire.Module module = parser.parseModule(moduleJsonByFilename.get(reference.resource()));
            validateModuleEnvelope(bundle.envelope(), module.envelope());
            if (!reference.id().equals(module.moduleId())) {
                fail(PolicyFailureCode.ENVELOPE_MISMATCH);
            }
            statementCount += module.statements().size();
            if (statementCount > MAX_STATEMENTS) {
                fail(PolicyFailureCode.STATEMENT_LIMIT_EXCEEDED);
            }
            modules.add(compileModule(reference, module, domainSchema, statementIds));
        }

        List<CompiledPolicyModule> canonicalModules = canonicalizer.canonicalize(modules);
        String fingerprint = fingerprintCalculator.calculate(
                envelope.schemaVersion(),
                envelope.contextSchemaVersion(),
                envelope.namespace(),
                envelope.policyVersion(),
                envelope.defaultEffect(),
                envelope.combiningAlgorithm(),
                canonicalModules);
        return new CompiledPolicyBundle(
                envelope.schemaVersion(),
                envelope.contextSchemaVersion(),
                envelope.namespace(),
                envelope.policyVersion(),
                envelope.defaultEffect(),
                envelope.combiningAlgorithm(),
                domainSchema,
                fingerprint,
                canonicalModules);
    }

    private CompiledPolicyModule compileModule(
            PolicySchema10Wire.ModuleReference reference,
            PolicySchema10Wire.Module module,
            PolicyDomainSchema domainSchema,
            Set<String> statementIds) {
        List<CompiledPolicyStatement> statements = new ArrayList<>();
        for (PolicySchema10Wire.Statement statement : module.statements()) {
            validateIdentifier(statement.id());
            if (!statementIds.add(statement.id())) {
                fail(PolicyFailureCode.DUPLICATE_STATEMENT);
            }
            statements.add(compileStatement(statement, domainSchema));
        }
        return new CompiledPolicyModule(reference.id(), reference.resource(), statements);
    }

    private CompiledPolicyStatement compileStatement(
            PolicySchema10Wire.Statement statement, PolicyDomainSchema domainSchema) {
        List<String> actions = compileActions(statement.actions(), domainSchema);
        PolicyEffect effect = parseEffect(statement.effect());
        ConditionBudget budget = new ConditionBudget();
        PolicyCondition condition = compileCondition(statement.condition(), domainSchema, 1, budget);
        List<CompiledPolicyOutcome> outcomes = compileOutcomes(statement.outcomes(), domainSchema);
        validateActionContracts(actions, condition, outcomes, domainSchema);
        if (effect == PolicyEffect.DENY && !outcomes.isEmpty()) {
            fail(PolicyFailureCode.INVALID_OUTCOME_TYPE);
        }
        return new CompiledPolicyStatement(statement.id(), actions, effect, condition, outcomes);
    }

    private List<String> compileActions(List<String> actions, PolicyDomainSchema domainSchema) {
        if (actions.isEmpty() || actions.size() > MAX_ACTIONS_PER_STATEMENT) {
            fail(PolicyFailureCode.ACTION_LIMIT_EXCEEDED);
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String action : actions) {
            validateIdentifier(action);
            if (domainSchema.action(action).isEmpty()) {
                fail(PolicyFailureCode.UNKNOWN_ACTION);
            }
            if (!unique.add(action)) {
                fail(PolicyFailureCode.INVALID_FIELD_TYPE);
            }
        }
        return List.copyOf(unique);
    }

    private PolicyCondition compileCondition(
            PolicySchema10Wire.Condition wire,
            PolicyDomainSchema domainSchema,
            int depth,
            ConditionBudget budget) {
        if (depth > MAX_CONDITION_DEPTH) {
            fail(PolicyFailureCode.CONDITION_DEPTH_LIMIT_EXCEEDED);
        }
        budget.addNode();
        if (wire instanceof PolicySchema10Wire.GroupCondition group) {
            if (group.conditions().isEmpty()) {
                fail(PolicyFailureCode.EMPTY_CONDITION);
            }
            if (group.conditions().size() > MAX_CONDITION_CHILDREN) {
                fail(PolicyFailureCode.CONDITION_CHILD_LIMIT_EXCEEDED);
            }
            List<PolicyCondition> children = group.conditions().stream()
                    .map(condition -> compileCondition(condition, domainSchema, depth + 1, budget))
                    .toList();
            return switch (group.type()) {
                case "ALL" -> new PolicyCondition.All(children);
                case "ANY" -> new PolicyCondition.Any(children);
                default -> throw new PolicyCompilationException(PolicyFailureCode.EMPTY_CONDITION);
            };
        }
        PolicySchema10Wire.PredicateCondition predicate = (PolicySchema10Wire.PredicateCondition) wire;
        PolicyOperator operator = parseOperator(predicate.operator());
        List<PolicyOperand> operands = new ArrayList<>();
        operands.add(compileOperand(predicate.left(), domainSchema));
        predicate.right().ifPresent(right -> operands.add(compileOperand(right, domainSchema)));
        validateOperatorSignature(operator, operands);
        validateEnumOperands(operands, domainSchema);
        return new PolicyCondition.Predicate(operator, operands);
    }

    private PolicyOperand compileOperand(PolicySchema10Wire.Operand wire, PolicyDomainSchema domainSchema) {
        if ("ATTRIBUTE".equals(wire.type())) {
            validateIdentifier(wire.name());
            AttributeSchema schema = domainSchema
                    .attribute(wire.name())
                    .orElseThrow(() -> new PolicyCompilationException(PolicyFailureCode.UNKNOWN_ATTRIBUTE));
            return new PolicyOperand.Attribute(schema.attributeName(), schema.type());
        }
        PolicyValueType type = parseValueType(wire.type());
        return new PolicyOperand.Literal(compileLiteral(type, wire.value()));
    }

    private List<CompiledPolicyOutcome> compileOutcomes(
            List<PolicySchema10Wire.Outcome> outcomes, PolicyDomainSchema domainSchema) {
        Set<String> keys = new HashSet<>();
        List<CompiledPolicyOutcome> compiled = new ArrayList<>();
        for (PolicySchema10Wire.Outcome outcome : outcomes) {
            validateIdentifier(outcome.key());
            if (!keys.add(outcome.key())) {
                fail(PolicyFailureCode.DUPLICATE_OUTCOME);
            }
            OutcomeSchema schema = domainSchema
                    .outcome(outcome.key())
                    .orElseThrow(() -> new PolicyCompilationException(PolicyFailureCode.UNKNOWN_OUTCOME));
            PolicyOperand value = compileOperand(outcome.value(), domainSchema);
            if (schema.type() != value.type()) {
                fail(PolicyFailureCode.INVALID_OUTCOME_TYPE);
            }
            validateOutcomeValue(schema, value, domainSchema);
            compiled.add(new CompiledPolicyOutcome(schema.outcomeKey(), value));
        }
        return List.copyOf(compiled);
    }

    private PolicyValue compileLiteral(PolicyValueType type, Object value) {
        return switch (type) {
            case BOOLEAN -> new PolicyValue.BooleanValue((Boolean) value);
            case LONG -> new PolicyValue.LongValue((Long) value);
            case STRING -> new PolicyValue.StringValue(validateString((String) value));
            case ENUM -> new PolicyValue.EnumValue(validateString((String) value));
            case INSTANT -> new PolicyValue.InstantValue(parseInstant((String) value));
            case LONG_SET -> new PolicyValue.LongSetValue(longSet(value));
            case STRING_SET -> new PolicyValue.StringSetValue(stringSet(value));
            case ENUM_SET -> new PolicyValue.EnumSetValue(stringSet(value));
            case INSTANT_SET -> new PolicyValue.InstantSetValue(instantSet(value));
        };
    }

    private void validateOperatorSignature(PolicyOperator operator, List<PolicyOperand> operands) {
        switch (operator) {
            case EXISTS, NOT_EXISTS -> {
                requireArity(operands, 1);
                if (!(operands.getFirst() instanceof PolicyOperand.Attribute)) {
                    fail(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
                }
            }
            case EQ, NEQ -> {
                requireArity(operands, 2);
                requireSameType(operands);
                if (!operands.getFirst().type().isScalar()) {
                    fail(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
                }
            }
            case IN -> {
                requireArity(operands, 2);
                requireScalarAndSet(operands.get(0), operands.get(1));
            }
            case CONTAINS -> {
                requireArity(operands, 2);
                requireScalarAndSet(operands.get(1), operands.get(0));
            }
            case INTERSECTS -> {
                requireArity(operands, 2);
                requireSameType(operands);
                if (!operands.getFirst().type().isSet()) {
                    fail(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
                }
            }
            case LT, LTE, GT, GTE -> {
                requireArity(operands, 2);
                requireSameType(operands);
                PolicyValueType type = operands.getFirst().type();
                if (type != PolicyValueType.LONG && type != PolicyValueType.INSTANT) {
                    fail(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
                }
            }
        }
    }

    private void validateOutcomeValue(
            OutcomeSchema schema, PolicyOperand operand, PolicyDomainSchema domainSchema) {
        if (operand instanceof PolicyOperand.Attribute attribute) {
            if (schema.type() == PolicyValueType.ENUM) {
                AttributeSchema attributeSchema = domainSchema
                        .attribute(attribute.name())
                        .orElseThrow(() -> new PolicyCompilationException(PolicyFailureCode.UNKNOWN_ATTRIBUTE));
                if (!schema.dominanceOrder().containsAll(attributeSchema.enumSymbols())) {
                    fail(PolicyFailureCode.UNKNOWN_ENUM_SYMBOL);
                }
            }
            return;
        }
        PolicyValue value = ((PolicyOperand.Literal) operand).value();
        if (schema.mergeStrategy() == OutcomeMergeStrategy.DOMINANCE || schema.type() == PolicyValueType.ENUM) {
            String candidate;
            if (value instanceof PolicyValue.EnumValue enumValue) {
                candidate = enumValue.value();
            } else if (value instanceof PolicyValue.StringValue stringValue) {
                candidate = stringValue.value();
            } else {
                fail(PolicyFailureCode.INVALID_OUTCOME_TYPE);
                return;
            }
            if (!schema.dominanceOrder().contains(candidate)) {
                fail(PolicyFailureCode.UNKNOWN_ENUM_SYMBOL);
            }
        }
    }

    private void validateActionContracts(
            List<String> actions,
            PolicyCondition condition,
            List<CompiledPolicyOutcome> outcomes,
            PolicyDomainSchema domainSchema) {
        Set<String> referencedAttributes = new HashSet<>();
        collectAttributes(condition, referencedAttributes);
        outcomes.stream()
                .map(CompiledPolicyOutcome::value)
                .filter(PolicyOperand.Attribute.class::isInstance)
                .map(PolicyOperand.Attribute.class::cast)
                .map(PolicyOperand.Attribute::name)
                .forEach(referencedAttributes::add);

        for (String actionId : actions) {
            ActionSchema action = domainSchema
                    .action(actionId)
                    .orElseThrow(() -> new PolicyCompilationException(PolicyFailureCode.UNKNOWN_ACTION));
            if (!action.allowedAttributes().containsAll(referencedAttributes)) {
                fail(PolicyFailureCode.ATTRIBUTE_NOT_ALLOWED_FOR_ACTION);
            }
            for (CompiledPolicyOutcome outcome : outcomes) {
                if (!action.allowedOutcomes().contains(outcome.key())) {
                    fail(PolicyFailureCode.OUTCOME_NOT_ALLOWED_FOR_ACTION);
                }
            }
        }
    }

    private void collectAttributes(PolicyCondition condition, Set<String> attributes) {
        if (condition instanceof PolicyCondition.All all) {
            all.children().forEach(child -> collectAttributes(child, attributes));
            return;
        }
        if (condition instanceof PolicyCondition.Any any) {
            any.children().forEach(child -> collectAttributes(child, attributes));
            return;
        }
        PolicyCondition.Predicate predicate = (PolicyCondition.Predicate) condition;
        predicate.operands().stream()
                .filter(PolicyOperand.Attribute.class::isInstance)
                .map(PolicyOperand.Attribute.class::cast)
                .map(PolicyOperand.Attribute::name)
                .forEach(attributes::add);
    }

    private void validateEnumOperands(List<PolicyOperand> operands, PolicyDomainSchema domainSchema) {
        for (PolicyOperand operand : operands) {
            if (!(operand instanceof PolicyOperand.Attribute attribute)
                    || (attribute.type() != PolicyValueType.ENUM
                            && attribute.type() != PolicyValueType.ENUM_SET)) {
                continue;
            }
            AttributeSchema schema = domainSchema
                    .attribute(attribute.name())
                    .orElseThrow(() -> new PolicyCompilationException(PolicyFailureCode.UNKNOWN_ATTRIBUTE));
            for (PolicyOperand candidate : operands) {
                if (candidate instanceof PolicyOperand.Literal literal) {
                    Set<String> symbols = enumSymbols(literal.value());
                    if (!schema.enumSymbols().containsAll(symbols)) {
                        fail(PolicyFailureCode.UNKNOWN_ENUM_SYMBOL);
                    }
                }
            }
        }
    }

    private Set<String> enumSymbols(PolicyValue value) {
        if (value instanceof PolicyValue.EnumValue enumValue) {
            return Set.of(enumValue.value());
        }
        if (value instanceof PolicyValue.EnumSetValue enumSetValue) {
            return enumSetValue.value();
        }
        return Set.of();
    }

    private EnvelopeValues compileEnvelope(PolicySchema10Wire.Bundle bundle, PolicyDomainSchema domainSchema) {
        PolicySchema10Wire.CommonEnvelope envelope = bundle.envelope();
        validateIdentifier(envelope.contextSchemaVersion());
        validateIdentifier(envelope.namespace());
        validateString(envelope.policyVersion());
        if (!SEMVER.matcher(envelope.policyVersion()).matches()) {
            fail(PolicyFailureCode.INVALID_POLICY_VERSION);
        }
        if (!domainSchema.contextSchemaVersion().equals(envelope.contextSchemaVersion())) {
            fail(PolicyFailureCode.ENVELOPE_MISMATCH);
        }
        PolicyEffect defaultEffect = parseEffect(bundle.defaultEffect());
        if (defaultEffect != PolicyEffect.DENY) {
            fail(PolicyFailureCode.ENVELOPE_MISMATCH);
        }
        PolicyCombiningAlgorithm combiningAlgorithm = parseCombining(bundle.combiningAlgorithm());
        return new EnvelopeValues(
                envelope.schemaVersion(),
                envelope.contextSchemaVersion(),
                envelope.namespace(),
                envelope.policyVersion(),
                defaultEffect,
                combiningAlgorithm);
    }

    private Map<String, PolicySchema10Wire.ModuleReference> validateReferences(
            List<PolicySchema10Wire.ModuleReference> references) {
        if (references.isEmpty()) {
            fail(PolicyFailureCode.MISSING_MODULE);
        }
        Map<String, PolicySchema10Wire.ModuleReference> byFilename = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        for (PolicySchema10Wire.ModuleReference reference : references) {
            validateIdentifier(reference.id());
            validateString(reference.resource());
            if (!reference.resource().equals(reference.id() + ".policy.json")) {
                fail(PolicyFailureCode.INVALID_MODULE_FILENAME);
            }
            if (!ids.add(reference.id()) || byFilename.putIfAbsent(reference.resource(), reference) != null) {
                fail(PolicyFailureCode.DUPLICATE_MODULE);
            }
        }
        return Map.copyOf(byFilename);
    }

    private void validateProvidedModules(
            Map<String, PolicySchema10Wire.ModuleReference> references, Map<String, byte[]> provided) {
        for (String filename : references.keySet()) {
            if (!provided.containsKey(filename)) {
                fail(PolicyFailureCode.MISSING_MODULE);
            }
        }
        for (String filename : provided.keySet()) {
            if (!references.containsKey(filename)) {
                fail(PolicyFailureCode.UNDECLARED_MODULE);
            }
        }
    }

    private void validateModuleEnvelope(
            PolicySchema10Wire.CommonEnvelope bundle, PolicySchema10Wire.CommonEnvelope module) {
        if (!bundle.schemaVersion().equals(module.schemaVersion())
                || !bundle.contextSchemaVersion().equals(module.contextSchemaVersion())
                || !bundle.namespace().equals(module.namespace())
                || !bundle.policyVersion().equals(module.policyVersion())) {
            fail(PolicyFailureCode.ENVELOPE_MISMATCH);
        }
    }

    private void enforceBundleSize(byte[] bundle, Map<String, byte[]> modules) {
        long totalBytes = bundle.length;
        for (byte[] module : modules.values()) {
            totalBytes += module.length;
            if (totalBytes > PolicyJsonParser.MAX_BUNDLE_BYTES) {
                fail(PolicyFailureCode.DOCUMENT_TOO_LARGE);
            }
        }
    }

    private Set<Long> longSet(Object value) {
        List<?> values = (List<?>) value;
        validateSetSize(values);
        Set<Long> result = new LinkedHashSet<>();
        values.forEach(element -> result.add((Long) element));
        return Set.copyOf(result);
    }

    private Set<String> stringSet(Object value) {
        List<?> values = (List<?>) value;
        validateSetSize(values);
        Set<String> result = new LinkedHashSet<>();
        values.forEach(element -> result.add(validateString((String) element)));
        return Set.copyOf(result);
    }

    private Set<Instant> instantSet(Object value) {
        List<?> values = (List<?>) value;
        validateSetSize(values);
        Set<Instant> result = new LinkedHashSet<>();
        values.forEach(element -> result.add(parseInstant((String) element)));
        return Set.copyOf(result);
    }

    private void validateSetSize(List<?> values) {
        if (values.size() > MAX_SET_VALUES) {
            fail(PolicyFailureCode.SET_LIMIT_EXCEEDED);
        }
    }

    private Instant parseInstant(String value) {
        validateString(value);
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new PolicyCompilationException(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
    }

    private String validateString(String value) {
        if (value.length() > MAX_STRING_LENGTH) {
            fail(PolicyFailureCode.STRING_LIMIT_EXCEEDED);
        }
        return value;
    }

    private void validateIdentifier(String value) {
        if (value.length() > MAX_IDENTIFIER_LENGTH) {
            fail(PolicyFailureCode.IDENTIFIER_LIMIT_EXCEEDED);
        }
        if (!IDENTIFIER.matcher(value).matches()) {
            fail(PolicyFailureCode.INVALID_IDENTIFIER);
        }
    }

    private PolicyEffect parseEffect(String value) {
        try {
            return PolicyEffect.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new PolicyCompilationException(PolicyFailureCode.INVALID_FIELD_TYPE);
        }
    }

    private PolicyCombiningAlgorithm parseCombining(String value) {
        try {
            return PolicyCombiningAlgorithm.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new PolicyCompilationException(PolicyFailureCode.ENVELOPE_MISMATCH);
        }
    }

    private PolicyOperator parseOperator(String value) {
        try {
            return PolicyOperator.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new PolicyCompilationException(PolicyFailureCode.UNKNOWN_OPERATOR);
        }
    }

    private PolicyValueType parseValueType(String value) {
        try {
            return PolicyValueType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new PolicyCompilationException(PolicyFailureCode.UNKNOWN_OPERAND_TYPE);
        }
    }

    private void requireArity(List<PolicyOperand> operands, int expected) {
        if (operands.size() != expected) {
            fail(PolicyFailureCode.INVALID_OPERATOR_ARITY);
        }
    }

    private void requireSameType(List<PolicyOperand> operands) {
        if (operands.get(0).type() != operands.get(1).type()) {
            fail(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
        }
    }

    private void requireScalarAndSet(PolicyOperand scalar, PolicyOperand set) {
        if (!scalar.type().isScalar()
                || scalar.type() == PolicyValueType.BOOLEAN
                || !set.type().isSet()
                || scalar.type().setType() != set.type()) {
            fail(PolicyFailureCode.OPERAND_TYPE_MISMATCH);
        }
    }

    private void fail(PolicyFailureCode code) {
        throw new PolicyCompilationException(code);
    }

    private record EnvelopeValues(
            String schemaVersion,
            String contextSchemaVersion,
            String namespace,
            String policyVersion,
            PolicyEffect defaultEffect,
            PolicyCombiningAlgorithm combiningAlgorithm) {}

    private static final class ConditionBudget {
        private int nodes;

        private void addNode() {
            nodes++;
            if (nodes > MAX_CONDITION_NODES) {
                throw new PolicyCompilationException(PolicyFailureCode.CONDITION_NODE_LIMIT_EXCEEDED);
            }
        }
    }
}
