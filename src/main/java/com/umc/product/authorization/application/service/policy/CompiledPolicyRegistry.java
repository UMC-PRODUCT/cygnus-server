package com.umc.product.authorization.application.service.policy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.policy.CompilePolicyBundleUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyClasspathResource;
import com.umc.product.authorization.application.port.out.policy.PolicyResourceManifest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;

@Component
public class CompiledPolicyRegistry {

    static final int MAX_BUNDLES = 64;
    static final int MAX_TOTAL_POLICY_BYTES = 16 * 1024 * 1024;
    static final int MAX_TOTAL_STATEMENTS = 5_000;

    private final Map<PolicyBundleKey, CompiledPolicyBundle> bundlesByKey;
    private final Map<String, PolicyBundleKey> activeKeyByNamespace;
    private final Map<String, PolicySurfaceDescriptor> surfacesByQualifiedId;
    private final Set<String> commonRolloutNamespaces;

    @Autowired
    public CompiledPolicyRegistry(
        CompilePolicyBundleUseCase compiler,
        List<PolicyBundleContributor> contributors
    ) {
        this(compiler, contributors, Thread.currentThread().getContextClassLoader());
    }

    CompiledPolicyRegistry(
        CompilePolicyBundleUseCase compiler,
        List<PolicyBundleContributor> contributors,
        ClassLoader classLoader
    ) {
        Objects.requireNonNull(compiler);
        Objects.requireNonNull(contributors);
        Objects.requireNonNull(classLoader);
        if (contributors.size() > MAX_BUNDLES) {
            throw new IllegalStateException("등록된 policy bundle 수가 전체 제한을 초과했습니다.");
        }

        Map<PolicyBundleKey, CompiledPolicyBundle> compiledByKey = new TreeMap<>();
        Map<String, PolicyBundleKey> activeByNamespace = new TreeMap<>();
        Map<String, PolicySurfaceDescriptor> surfaces = new TreeMap<>();
        Set<String> rolloutNamespaces = new TreeSet<>();
        long totalBytes = 0;
        int totalStatements = 0;
        List<PolicyBundleContributor> ordered = new ArrayList<>(contributors);
        ordered.sort(Comparator.comparing(PolicyBundleContributor::namespace));

        for (PolicyBundleContributor contributor : ordered) {
            String namespace = requireNamespace(contributor);
            if (activeByNamespace.containsKey(namespace)) {
                throw new IllegalStateException("Policy namespace가 중복되었습니다: " + namespace);
            }
            LoadedPolicyResources resources = load(classLoader, contributor.resourceManifest());
            totalBytes += resources.totalBytes();
            if (totalBytes > MAX_TOTAL_POLICY_BYTES) {
                throw new IllegalStateException("전체 policy resource 크기 제한을 초과했습니다.");
            }

            CompiledPolicyBundle bundle = compiler.compile(new PolicyBundleCompilationRequest(
                resources.bundle(),
                resources.modules(),
                contributor.domainSchema()));
            if (!namespace.equals(bundle.namespace())) {
                throw new IllegalStateException("Contributor와 bundle namespace가 일치하지 않습니다: " + namespace);
            }
            contributor.compiledContractValidator().validate(bundle);
            validateSurfaces(contributor, bundle, surfaces);
            if (contributor.commonRolloutEnabled()) {
                rolloutNamespaces.add(namespace);
            }
            totalStatements += bundle.modules().stream()
                .mapToInt(module -> module.statements().size())
                .sum();
            if (totalStatements > MAX_TOTAL_STATEMENTS) {
                throw new IllegalStateException("전체 policy statement 수 제한을 초과했습니다.");
            }

            PolicyBundleKey key = new PolicyBundleKey(bundle.namespace(), bundle.contextSchemaVersion());
            if (compiledByKey.putIfAbsent(key, bundle) != null) {
                throw new IllegalStateException("Policy bundle key가 중복되었습니다: " + key);
            }
            activeByNamespace.put(namespace, key);
        }

        this.bundlesByKey = Collections.unmodifiableMap(new TreeMap<>(compiledByKey));
        this.activeKeyByNamespace = Collections.unmodifiableMap(new TreeMap<>(activeByNamespace));
        this.surfacesByQualifiedId = Collections.unmodifiableMap(new TreeMap<>(surfaces));
        this.commonRolloutNamespaces = Collections.unmodifiableSet(new TreeSet<>(rolloutNamespaces));
    }

    public CompiledPolicyBundle require(String namespace) {
        PolicyBundleKey key = activeKeyByNamespace.get(Objects.requireNonNull(namespace));
        if (key == null) {
            throw new IllegalStateException("등록된 active policy bundle이 없습니다: " + namespace);
        }
        return require(key);
    }

    public CompiledPolicyBundle require(PolicyBundleKey key) {
        CompiledPolicyBundle bundle = bundlesByKey.get(Objects.requireNonNull(key));
        if (bundle == null) {
            throw new IllegalStateException("등록된 policy bundle이 없습니다: " + key);
        }
        return bundle;
    }

    public Map<PolicyBundleKey, CompiledPolicyBundle> bundles() {
        return bundlesByKey;
    }

    public List<PolicySurfaceDescriptor> surfaces() {
        return List.copyOf(surfacesByQualifiedId.values());
    }

    public Set<String> commonRolloutNamespaces() {
        return commonRolloutNamespaces;
    }

    private void validateSurfaces(
        PolicyBundleContributor contributor,
        CompiledPolicyBundle bundle,
        Map<String, PolicySurfaceDescriptor> surfaces
    ) {
        for (PolicySurfaceDescriptor surface : contributor.policySurfaces()) {
            if (!bundle.namespace().equals(surface.namespace())) {
                throw new IllegalStateException(
                    "Policy contributor와 surface namespace가 일치하지 않습니다: " + surface.id());
            }
            if (bundle.domainSchema().action(surface.actionId()).isEmpty()) {
                throw new IllegalStateException(
                    "Policy surface action이 domain schema에 없습니다: " + surface.actionId());
            }
            boolean moduleExists = bundle.modules().stream()
                .anyMatch(module -> module.id().equals(surface.moduleId()));
            if (!moduleExists) {
                throw new IllegalStateException(
                    "Policy surface module이 compiled bundle에 없습니다: " + surface.moduleId());
            }
            if (bundle.statementsForAction(surface.actionId()).isEmpty()) {
                throw new IllegalStateException(
                    "Policy surface action에 compiled statement가 없습니다: " + surface.actionId());
            }
            String qualifiedId = surface.namespace() + ":" + surface.id();
            if (surfaces.putIfAbsent(qualifiedId, surface) != null) {
                throw new IllegalStateException("Policy surface ID가 중복되었습니다: " + qualifiedId);
            }
        }
    }

    private String requireNamespace(PolicyBundleContributor contributor) {
        String namespace = Objects.requireNonNull(contributor).namespace();
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException("Policy contributor namespace는 비어 있을 수 없습니다.");
        }
        return namespace;
    }

    private LoadedPolicyResources load(ClassLoader classLoader, PolicyResourceManifest manifest) {
        byte[] bundle = readExactlyOne(classLoader, manifest.bundle());
        Map<String, byte[]> modules = new LinkedHashMap<>();
        long totalBytes = bundle.length;
        for (PolicyClasspathResource module : manifest.modules()) {
            byte[] content = readExactlyOne(classLoader, module);
            modules.put(module.logicalFilename(), content);
            totalBytes += content.length;
        }
        return new LoadedPolicyResources(bundle, modules, totalBytes);
    }

    private byte[] readExactlyOne(ClassLoader classLoader, PolicyClasspathResource resource) {
        try {
            Enumeration<URL> resources = classLoader.getResources(resource.classpathPath());
            if (!resources.hasMoreElements()) {
                throw new IllegalStateException(
                    "Policy classpath resource가 없습니다: " + resource.classpathPath());
            }
            URL url = resources.nextElement();
            if (resources.hasMoreElements()) {
                throw new IllegalStateException(
                    "Policy classpath resource가 중복되었습니다: " + resource.classpathPath());
            }
            try (InputStream input = url.openStream()) {
                byte[] content = input.readNBytes(MAX_TOTAL_POLICY_BYTES + 1);
                if (content.length > MAX_TOTAL_POLICY_BYTES) {
                    throw new IllegalStateException(
                        "Policy classpath resource 크기 제한을 초과했습니다: " + resource.classpathPath());
                }
                return content;
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Policy classpath resource를 읽지 못했습니다: " + resource.classpathPath(), exception);
        }
    }

    private record LoadedPolicyResources(
        byte[] bundle,
        Map<String, byte[]> modules,
        long totalBytes
    ) {
        private LoadedPolicyResources {
            bundle = bundle.clone();
            Map<String, byte[]> copied = new LinkedHashMap<>();
            modules.forEach((filename, content) -> copied.put(filename, content.clone()));
            modules = Map.copyOf(copied);
        }
    }
}
