package com.umc.product.authorization.application.port.out.policy;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PolicyResourceManifest(
        PolicyClasspathResource bundle,
        List<PolicyClasspathResource> modules) {

    public PolicyResourceManifest {
        Objects.requireNonNull(bundle);
        modules = List.copyOf(modules);
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("Policy manifest must contain at least one module");
        }
        Set<String> logicalFilenames = new HashSet<>();
        Set<String> classpathPaths = new HashSet<>();
        addUnique(bundle, logicalFilenames, classpathPaths);
        modules.forEach(resource -> addUnique(resource, logicalFilenames, classpathPaths));
    }

    private static void addUnique(
        PolicyClasspathResource resource,
        Set<String> logicalFilenames,
        Set<String> classpathPaths
    ) {
        Objects.requireNonNull(resource);
        if (!logicalFilenames.add(resource.logicalFilename())) {
            throw new IllegalArgumentException("Policy logical filename is duplicated");
        }
        if (!classpathPaths.add(resource.classpathPath())) {
            throw new IllegalArgumentException("Policy classpath path is duplicated");
        }
    }
}
