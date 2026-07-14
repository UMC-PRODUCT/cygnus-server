package com.umc.product.project.application.authorization.rollout;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Objects;

public final class ProjectAuthorizationEnforcementReceiptLoader {

    static final String RECEIPT_PATH = "policies/project/rollout/enforcement-receipts.json";
    static final String ARTIFACT_PATH = "policies/project/generated/project-policy-artifacts.md";

    private final LoadedProjectAuthorizationEnforcementReceipts loaded;

    public ProjectAuthorizationEnforcementReceiptLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    ProjectAuthorizationEnforcementReceiptLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader);
        ProjectAuthorizationEnforcementReceiptDocument document = readReceipt(classLoader);
        byte[] artifact = readArtifact(classLoader);
        loaded = new LoadedProjectAuthorizationEnforcementReceipts(document, sha256(artifact));
    }

    public LoadedProjectAuthorizationEnforcementReceipts load() {
        return loaded;
    }

    private ProjectAuthorizationEnforcementReceiptDocument readReceipt(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources(RECEIPT_PATH);
            if (!resources.hasMoreElements()) {
                return ProjectAuthorizationEnforcementReceiptDocument.empty();
            }
            URL resource = resources.nextElement();
            if (resources.hasMoreElements()) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_RESOURCE_DUPLICATED);
            }
            try (InputStream input = resource.openStream()) {
                byte[] bytes = input.readNBytes(ProjectAuthorizationEnforcementReceiptJsonParser.MAX_BYTES + 1);
                return new ProjectAuthorizationEnforcementReceiptJsonParser().parse(bytes);
            }
        } catch (ProjectAuthorizationRolloutConfigurationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalid(ProjectAuthorizationRolloutFailureCode.RECEIPT_RESOURCE_READ_FAILED);
        }
    }

    private byte[] readArtifact(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources(ARTIFACT_PATH);
            if (!resources.hasMoreElements()) {
                fail(ProjectAuthorizationRolloutFailureCode.POLICY_ARTIFACT_RESOURCE_MISSING);
            }
            URL resource = resources.nextElement();
            if (resources.hasMoreElements()) {
                fail(ProjectAuthorizationRolloutFailureCode.POLICY_ARTIFACT_RESOURCE_DUPLICATED);
            }
            try (InputStream input = resource.openStream()) {
                return input.readAllBytes();
            }
        } catch (ProjectAuthorizationRolloutConfigurationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalid(ProjectAuthorizationRolloutFailureCode.POLICY_ARTIFACT_RESOURCE_READ_FAILED);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw invalid(ProjectAuthorizationRolloutFailureCode.POLICY_ARTIFACT_SHA256_UNAVAILABLE);
        }
    }

    private static ProjectAuthorizationRolloutConfigurationException invalid(
        ProjectAuthorizationRolloutFailureCode code
    ) {
        return new ProjectAuthorizationRolloutConfigurationException(code);
    }

    private static void fail(ProjectAuthorizationRolloutFailureCode code) {
        throw invalid(code);
    }
}
