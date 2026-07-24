package com.umc.product.project.application.authorization;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.policy.CompilePolicyBundleUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

@Component
public class ProjectPolicyBundleLoader {

    private final ProjectCompiledPolicyBundle compiled;

    @Autowired
    public ProjectPolicyBundleLoader(CompiledPolicyRegistry registry) {
        this.compiled = new ProjectCompiledPolicyBundle(registry.require("project"));
    }

    public ProjectPolicyBundleLoader(CompilePolicyBundleUseCase compiler) {
        this(compiler, Thread.currentThread().getContextClassLoader());
    }

    ProjectPolicyBundleLoader(CompilePolicyBundleUseCase compiler, ClassLoader classLoader) {
        byte[] bundle = readExactlyOne(classLoader, ProjectPolicyResourceManifest.BUNDLE.classpathPath());
        Map<String, byte[]> modules = new LinkedHashMap<>();
        for (ProjectPolicyResourceManifest.PolicyResource resource : ProjectPolicyResourceManifest.MODULES) {
            modules.put(resource.logicalFilename(), readExactlyOne(classLoader, resource.classpathPath()));
        }
        CompiledPolicyBundle value = compiler.compile(new PolicyBundleCompilationRequest(
            bundle, modules, ProjectPolicyDomainSchema.create()));
        new ProjectPolicyCompiledContractValidator().validate(value);
        this.compiled = new ProjectCompiledPolicyBundle(value);
    }

    public ProjectCompiledPolicyBundle compiled() {
        return compiled;
    }

    private byte[] readExactlyOne(ClassLoader classLoader, String path) {
        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            if (!resources.hasMoreElements()) {
                throw new IllegalStateException("Project policy classpath resource가 없습니다: " + path);
            }
            URL resource = resources.nextElement();
            if (resources.hasMoreElements()) {
                throw new IllegalStateException("Project policy classpath resource가 중복되었습니다: " + path);
            }
            try (InputStream input = resource.openStream()) {
                return input.readAllBytes();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Project policy classpath resource를 읽지 못했습니다: " + path, exception);
        }
    }
}
