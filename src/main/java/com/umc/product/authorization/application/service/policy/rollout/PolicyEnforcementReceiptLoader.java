package com.umc.product.authorization.application.service.policy.rollout;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class PolicyEnforcementReceiptLoader {

    private final ClassLoader classLoader;

    public PolicyEnforcementReceiptLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    PolicyEnforcementReceiptLoader(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader);
    }

    public LoadedPolicyEnforcementReceipts load(String namespace) {
        String root = "policies/" + namespace;
        byte[] receipt = readExactlyOne(
            root + "/rollout/enforcement-receipts.json",
            PolicyEnforcementReceiptJsonParser.MAX_BYTES);
        byte[] artifact = readExactlyOne(
            root + "/generated/" + namespace + "-policy-artifacts.md",
            1024 * 1024);
        return new LoadedPolicyEnforcementReceipts(
            new PolicyEnforcementReceiptJsonParser().parse(receipt),
            sha256(artifact));
    }

    private byte[] readExactlyOne(String path, int maxBytes) {
        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            if (!resources.hasMoreElements()) {
                throw new IllegalStateException("Policy rollout resource가 없습니다: " + path);
            }
            URL resource = resources.nextElement();
            if (resources.hasMoreElements()) {
                throw new IllegalStateException("Policy rollout resource가 중복되었습니다: " + path);
            }
            try (InputStream input = resource.openStream()) {
                byte[] bytes = input.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw new IllegalStateException("Policy rollout resource 크기 제한을 초과했습니다: " + path);
                }
                return bytes;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Policy rollout resource를 읽지 못했습니다: " + path, exception);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
