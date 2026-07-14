package com.umc.product.project.application.authorization;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

final class ProjectPolicyRuntimeSurfaceDiscovery {

    static final String REST_PACKAGE = "com.umc.product.project.adapter.in.web";
    private static final String GRAPHQL_PACKAGE = "com.umc.product.project.adapter.in.graphql";
    private static final String SCHEDULER_PACKAGE = "com.umc.product.project.adapter.in.scheduler";

    private ProjectPolicyRuntimeSurfaceDiscovery() {
    }

    static Set<ProjectPolicySurfaceIdentity> discoverAnnotatedSurfaces(Environment environment) {
        Set<ProjectPolicySurfaceIdentity> surfaces = new LinkedHashSet<>();
        restControllers(environment).forEach(controller -> discoverRest(controller, surfaces));
        graphqlControllers(environment).forEach(controller -> discoverGraphQl(controller, surfaces));
        schedulerComponents(environment).forEach(component -> discoverScheduler(component, surfaces));
        return Set.copyOf(surfaces);
    }

    static Set<Class<?>> restControllers(Environment environment) {
        return scan(REST_PACKAGE, RestController.class, environment);
    }

    static boolean isProjectRestController(Class<?> candidate) {
        return candidate.getPackageName().startsWith(REST_PACKAGE)
            && AnnotatedElementUtils.hasAnnotation(candidate, RestController.class);
    }

    private static Set<Class<?>> graphqlControllers(Environment environment) {
        return scan(GRAPHQL_PACKAGE, Controller.class, environment);
    }

    private static Set<Class<?>> schedulerComponents(Environment environment) {
        return scan(SCHEDULER_PACKAGE, Component.class, environment);
    }

    private static Set<Class<?>> scan(
        String basePackage,
        Class<? extends java.lang.annotation.Annotation> stereotype,
        Environment environment
    ) {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter(new AnnotationTypeFilter(stereotype));

        Set<Class<?>> classes = new LinkedHashSet<>();
        scanner.findCandidateComponents(basePackage).stream()
            .map(BeanDefinition::getBeanClassName)
            .sorted()
            .map(ProjectPolicyRuntimeSurfaceDiscovery::loadClass)
            .forEach(classes::add);
        return Set.copyOf(classes);
    }

    private static void discoverRest(Class<?> controller, Set<ProjectPolicySurfaceIdentity> surfaces) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        String basePath = mappingPath(classMapping);
        for (Method method : controller.getDeclaredMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) {
                continue;
            }
            for (RequestMethod requestMethod : mapping.method()) {
                surfaces.add(new ProjectPolicySurfaceIdentity(
                    "rest:" + requestMethod.name() + " " + basePath + mappingPath(mapping),
                    controller.getName() + "#" + method.getName()
                ));
            }
        }
    }

    private static void discoverGraphQl(Class<?> controller, Set<ProjectPolicySurfaceIdentity> surfaces) {
        for (Method method : controller.getDeclaredMethods()) {
            QueryMapping query = AnnotatedElementUtils.findMergedAnnotation(method, QueryMapping.class);
            if (query != null) {
                String field = query.name().isBlank() ? method.getName() : query.name();
                surfaces.add(new ProjectPolicySurfaceIdentity(
                    "graphql:Query." + field,
                    controller.getName() + "#" + method.getName()
                ));
            }
            BatchMapping batch = AnnotatedElementUtils.findMergedAnnotation(method, BatchMapping.class);
            if (batch != null) {
                String field = batch.field().isBlank() ? method.getName() : batch.field();
                surfaces.add(new ProjectPolicySurfaceIdentity(
                    "graphql:" + batch.typeName() + "." + field,
                    controller.getName() + "#" + method.getName()
                ));
            }
        }
    }

    private static void discoverScheduler(Class<?> component, Set<ProjectPolicySurfaceIdentity> surfaces) {
        for (Method method : component.getDeclaredMethods()) {
            if (!method.getName().equals("handle") || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            String simpleName = component.getSimpleName().replaceFirst("Handler$", "");
            surfaces.add(new ProjectPolicySurfaceIdentity(
                "scheduler:" + simpleName.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(),
                component.getName() + "#" + method.getName()
            ));
        }
    }

    private static String mappingPath(RequestMapping mapping) {
        if (mapping == null) {
            return "";
        }
        String[] paths = mapping.path().length == 0 ? mapping.value() : mapping.path();
        return paths.length == 0 ? "" : paths[0];
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Project adapter class를 읽을 수 없습니다: " + className, exception);
        }
    }
}
