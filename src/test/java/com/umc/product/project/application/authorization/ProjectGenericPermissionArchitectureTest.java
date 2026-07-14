package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.project.adapter.in.web.ProjectQueryController;
import com.umc.product.project.adapter.in.web.assembler.ProjectResponseAssembler;
import com.umc.product.project.application.access.ProjectAccessScopeResolver;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.service.query.ProjectQueryService;

class ProjectGenericPermissionArchitectureTest {

    @Test
    @DisplayName("resource 없는 generic READ는 public project 목록의 exact scope precheck 한 곳뿐이다")
    void nullResourceReadIsPairedWithPublicProjectListScope() {
        List<Method> prechecks = ProjectPolicySurfaceCatalog.surfaces().stream()
            .map(ProjectPolicySurfaceDescriptor::handler)
            .map(this::resolveHandler)
            .filter(this::isGenericProjectRead)
            .toList();

        assertThat(prechecks).containsExactly(searchProjectsMethod());
        ProjectAuthorizationSurfaceBinding binding = prechecks.getFirst()
            .getDeclaredAnnotation(ProjectAuthorizationSurfaceBinding.class);
        assertThat(binding.value()).isEqualTo(ProjectAuthorizationSurface.REST_PROJECT_LIST_PUBLIC);
        assertThat(binding.value().action()).isEqualTo(ProjectPolicyAction.PROJECT_LIST_PUBLIC);

        assertThat(calls(ProjectQueryController.class, "searchProjects"))
            .contains(new MethodCall(ProjectResponseAssembler.class, "searchFor"));
        assertThat(calls(ProjectResponseAssembler.class, "searchFor"))
            .contains(new MethodCall(SearchProjectUseCase.class, "search"));
        assertThat(calls(ProjectQueryService.class, "search"))
            .contains(new MethodCall(ProjectAccessScopeResolver.class, "resolveForPublicSearch"));
        assertThat(actionConstants(ProjectAccessScopeResolver.class, "resolveForPublicSearch"))
            .containsExactly(ProjectPolicyAction.PROJECT_LIST_PUBLIC);
    }

    private Method searchProjectsMethod() {
        return java.util.Arrays.stream(ProjectQueryController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("searchProjects"))
            .findFirst()
            .orElseThrow();
    }

    private Method resolveHandler(String handler) {
        int separator = handler.lastIndexOf('#');
        String className = handler.substring(0, separator);
        String methodName = handler.substring(separator + 1);
        try {
            return java.util.Arrays.stream(Class.forName(className).getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Project surface handler를 찾을 수 없습니다: " + handler, exception);
        }
    }

    private boolean isGenericProjectRead(Method method) {
        CheckAccess access = method.getDeclaredAnnotation(CheckAccess.class);
        return access != null
            && access.resourceType() == ResourceType.PROJECT
            && access.permission() == PermissionType.READ
            && access.resourceId().isBlank()
            && access.action().isBlank();
    }

    private List<MethodCall> calls(Class<?> type, String methodName) {
        List<MethodCall> calls = new ArrayList<>();
        visit(type, new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                boolean isInterface) {
                try {
                    calls.add(new MethodCall(Class.forName(owner.replace('/', '.')), name));
                } catch (ClassNotFoundException ignored) {
                    return;
                }
            }
        }, methodName);
        return List.copyOf(calls);
    }

    private List<ProjectPolicyAction> actionConstants(Class<?> type, String methodName) {
        List<ProjectPolicyAction> actions = new ArrayList<>();
        visit(type, new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (opcode == Opcodes.GETSTATIC
                    && owner.equals(ProjectPolicyAction.class.getName().replace('.', '/'))) {
                    actions.add(ProjectPolicyAction.valueOf(name));
                }
            }
        }, methodName);
        return List.copyOf(actions);
    }

    private void visit(Class<?> type, MethodVisitor visitor, String selectedMethod) {
        try (InputStream input = type.getResourceAsStream('/' + type.getName().replace('.', '/') + ".class")) {
            if (input == null) {
                throw new IllegalStateException("class bytecode를 찾을 수 없습니다: " + type.getName());
            }
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    return name.equals(selectedMethod) ? visitor : null;
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private record MethodCall(Class<?> owner, String name) { }
}
