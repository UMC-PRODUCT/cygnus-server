package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;

import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Binding;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Caller;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutCoordinator;

class ProjectPolicyActionCallerArchitectureTest {

    private static final String COORDINATOR_OWNER = internalName(ProjectAuthorizationRolloutCoordinator.class);

    @Test
    @DisplayName("direct 19와 semantic 20 production contract는 39개 action을 중복 없이 완성한다")
    void direct와_semantic_production_contract가_exact39를_완성한다() {
        Set<ProjectPolicyAction> directActions = ProjectDirectActionBindings.actions();
        Set<ProjectPolicyAction> semanticActions = ProjectSemanticActionBindings.actions();
        EnumSet<ProjectPolicyAction> allActions = EnumSet.copyOf(directActions);
        allActions.addAll(semanticActions);

        assertThat(ProjectDirectActionBindings.values()).hasSize(19);
        assertThat(ProjectDirectActionBindings.values().stream().map(Binding::caller).distinct())
            .containsExactlyInAnyOrder(Caller.values());
        assertThat(semanticActions).hasSize(20);
        assertThat(directActions).doesNotContainAnyElementsOf(semanticActions);
        assertThat(allActions).containsExactlyInAnyOrder(ProjectPolicyAction.values());
    }

    @Test
    @DisplayName("production 권한 판정은 policy service의 단일 coordinator seam만 사용한다")
    void productionAuthorizationUsesOneCoordinatorSeam() throws IOException {
        Path projectRoot = Path.of("src/main/java/com/umc/product/project");
        int coordinatorCalls;
        try (var paths = Files.walk(projectRoot)) {
            coordinatorCalls = paths
                .filter(path -> path.toString().endsWith(".java"))
                .map(projectRoot::relativize)
                .map(ProjectPolicyActionCallerArchitectureTest::classResourceName)
                .mapToInt(ProjectPolicyActionCallerArchitectureTest::coordinatorInvocationCount)
                .sum();
        }

        assertThat(coordinatorCalls).isOne();
        assertThat(coordinatorInvocationCount(classResourceName(ProjectPolicyAuthorizationService.class))).isOne();
    }

    private static int coordinatorInvocationCount(String resourceName) {
        try (InputStream input = ProjectPolicyActionCallerArchitectureTest.class
            .getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                return 0;
            }
            CoordinatorCallCollector collector = new CoordinatorCallCollector();
            new ClassReader(input).accept(collector, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return collector.invocationCount();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String classResourceName(Class<?> type) {
        return internalName(type) + ".class";
    }

    private static String classResourceName(Path relativeSource) {
        String path = relativeSource.toString().replace('\\', '/');
        return "com/umc/product/project/" + path.substring(0, path.length() - ".java".length()) + ".class";
    }

    private static String internalName(Class<?> type) {
        return type.getName().replace('.', '/');
    }

    private static final class CoordinatorCallCollector extends ClassVisitor {

        private int invocationCount;

        private CoordinatorCallCollector() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions
        ) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(
                    int opcode,
                    String owner,
                    String methodName,
                    String methodDescriptor,
                    boolean isInterface
                ) {
                    if (owner.equals(COORDINATOR_OWNER) && methodName.equals("coordinate")) {
                        invocationCount++;
                    }
                }
            };
        }

        private int invocationCount() {
            return invocationCount;
        }
    }
}
