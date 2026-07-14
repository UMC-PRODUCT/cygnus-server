package com.umc.product.authorization.application.service.policy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.PolicyCombiningAlgorithm;
import com.umc.product.authorization.domain.policy.PolicyCondition;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyValue;

final class PolicyCanonicalJsonWriter {

    private final JsonFactory factory = new JsonFactory();

    String writeBundle(
            String schemaVersion,
            String contextSchemaVersion,
            String namespace,
            String policyVersion,
            PolicyEffect defaultEffect,
            PolicyCombiningAlgorithm combiningAlgorithm,
            List<CompiledPolicyModule> modules) {
        return write(generator -> {
            generator.writeStartObject();
            generator.writeStringField("schemaVersion", schemaVersion);
            generator.writeStringField("contextSchemaVersion", contextSchemaVersion);
            generator.writeStringField("namespace", namespace);
            generator.writeStringField("policyVersion", policyVersion);
            generator.writeStringField("defaultEffect", defaultEffect.name());
            generator.writeStringField("combiningAlgorithm", combiningAlgorithm.name());
            generator.writeArrayFieldStart("modules");
            for (CompiledPolicyModule module : modules) {
                writeModule(generator, module);
            }
            generator.writeEndArray();
            generator.writeEndObject();
        });
    }

    String writeCondition(PolicyCondition condition) {
        return write(generator -> writeCondition(generator, condition));
    }

    private String write(GeneratorAction action) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                JsonGenerator generator = factory.createGenerator(output)) {
            action.write(generator);
            generator.flush();
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Canonical policy JSON serialization failed", exception);
        }
    }

    private void writeModule(JsonGenerator generator, CompiledPolicyModule module) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("id", module.id());
        generator.writeStringField("filename", module.filename());
        generator.writeArrayFieldStart("statements");
        for (CompiledPolicyStatement statement : module.statements()) {
            writeStatement(generator, statement);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeStatement(JsonGenerator generator, CompiledPolicyStatement statement) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("id", statement.id());
        generator.writeStringField("effect", statement.effect().name());
        generator.writeArrayFieldStart("actions");
        for (String action : statement.actions()) {
            generator.writeString(action);
        }
        generator.writeEndArray();
        generator.writeFieldName("condition");
        writeCondition(generator, statement.condition());
        generator.writeArrayFieldStart("outcomes");
        for (CompiledPolicyOutcome outcome : statement.outcomes()) {
            generator.writeStartObject();
            generator.writeStringField("key", outcome.key());
            generator.writeFieldName("value");
            writeOperand(generator, outcome.value());
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeCondition(JsonGenerator generator, PolicyCondition condition) throws IOException {
        generator.writeStartObject();
        if (condition instanceof PolicyCondition.All all) {
            generator.writeStringField("type", "ALL");
            writeChildren(generator, all.children());
        } else if (condition instanceof PolicyCondition.Any any) {
            generator.writeStringField("type", "ANY");
            writeChildren(generator, any.children());
        } else {
            PolicyCondition.Predicate predicate = (PolicyCondition.Predicate) condition;
            generator.writeStringField("type", "PREDICATE");
            generator.writeStringField("operator", predicate.operator().name());
            generator.writeArrayFieldStart("operands");
            for (PolicyOperand operand : predicate.operands()) {
                writeOperand(generator, operand);
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
    }

    private void writeChildren(JsonGenerator generator, List<PolicyCondition> children) throws IOException {
        generator.writeArrayFieldStart("conditions");
        for (PolicyCondition child : children) {
            writeCondition(generator, child);
        }
        generator.writeEndArray();
    }

    private void writeOperand(JsonGenerator generator, PolicyOperand operand) throws IOException {
        generator.writeStartObject();
        if (operand instanceof PolicyOperand.Attribute attribute) {
            generator.writeStringField("type", "ATTRIBUTE");
            generator.writeStringField("name", attribute.name());
            generator.writeStringField("valueType", attribute.type().name());
        } else {
            PolicyValue value = ((PolicyOperand.Literal) operand).value();
            generator.writeStringField("type", value.type().name());
            writeValue(generator, value);
        }
        generator.writeEndObject();
    }

    private void writeValue(JsonGenerator generator, PolicyValue value) throws IOException {
        if (value instanceof PolicyValue.BooleanValue booleanValue) {
            generator.writeBooleanField("value", booleanValue.value());
        } else if (value instanceof PolicyValue.LongValue longValue) {
            generator.writeNumberField("value", longValue.value());
        } else if (value instanceof PolicyValue.StringValue stringValue) {
            generator.writeStringField("value", stringValue.value());
        } else if (value instanceof PolicyValue.EnumValue enumValue) {
            generator.writeStringField("value", enumValue.value());
        } else if (value instanceof PolicyValue.InstantValue instantValue) {
            generator.writeStringField("value", instantValue.value().toString());
        } else {
            writeSet(generator, value);
        }
    }

    private void writeSet(JsonGenerator generator, PolicyValue value) throws IOException {
        generator.writeArrayFieldStart("values");
        if (value instanceof PolicyValue.LongSetValue longSet) {
            for (Long element : longSet.value()) {
                generator.writeNumber(element);
            }
        } else if (value instanceof PolicyValue.StringSetValue stringSet) {
            for (String element : stringSet.value()) {
                generator.writeString(element);
            }
        } else if (value instanceof PolicyValue.EnumSetValue enumSet) {
            for (String element : enumSet.value()) {
                generator.writeString(element);
            }
        } else {
            PolicyValue.InstantSetValue instantSet = (PolicyValue.InstantSetValue) value;
            for (java.time.Instant element : instantSet.value()) {
                generator.writeString(element.toString());
            }
        }
        generator.writeEndArray();
    }

    @FunctionalInterface
    private interface GeneratorAction {
        void write(JsonGenerator generator) throws IOException;
    }
}
