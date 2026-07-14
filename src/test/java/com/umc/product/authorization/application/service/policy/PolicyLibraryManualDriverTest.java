package com.umc.product.authorization.application.service.policy;

import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.failure;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.request;
import static com.umc.product.authorization.application.service.policy.PolicyCompilerTestFixture.schema;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyCompilationException;
import com.umc.product.authorization.domain.policy.PolicyFailureCode;

class PolicyLibraryManualDriverTest {

    private static final String VALID_BUNDLE = "policies/task-2/valid/bundle.policy.json";
    private static final String VALID_MODULE = "policies/task-2/valid/demo.policy.json";
    private static final String DUPLICATE_BUNDLE =
            "policies/task-2/malformed/duplicate-key.bundle.policy.json";
    private static final String UNKNOWN_BUNDLE =
            "policies/task-2/malformed/unknown-field.bundle.policy.json";

    @DisplayName("library 사용자가 in-memory bundle을 컴파일하고 malformed bundle 코드를 확인한다")
    @Test
    void runsLibrarySurfaceDriver() throws IOException {
        // given
        PolicySemanticCompiler compiler = new PolicySemanticCompiler();
        byte[] module = resource(VALID_MODULE);

        // when
        CompiledPolicyBundle compiled = compiler.compile(compilationRequest(resource(VALID_BUNDLE), module));
        PolicyCompilationException duplicate = failure(
                compiler, compilationRequest(resource(DUPLICATE_BUNDLE), module));
        PolicyCompilationException unknown = failure(
                compiler, compilationRequest(resource(UNKNOWN_BUNDLE), module));

        // then
        assertThat(compiled.modules()).hasSize(1);
        assertThat(duplicate.code()).isEqualTo(PolicyFailureCode.DUPLICATE_KEY);
        assertThat(unknown.code()).isEqualTo(PolicyFailureCode.UNKNOWN_FIELD);
        assertThat(duplicate.getMessage()).doesNotContain("schemaVersion");
        assertThat(unknown.getMessage()).doesNotContain("sensitive-marker");

        System.out.printf(
                "MANUAL_VALID modules=%d statements=%d%n",
                compiled.modules().size(), compiled.modules().getFirst().statements().size());
        System.out.printf("MANUAL_DUPLICATE code=%s sanitized=true%n", duplicate.code());
        System.out.printf("MANUAL_UNKNOWN code=%s sanitized=true%n", unknown.code());
    }

    private PolicyBundleCompilationRequest compilationRequest(byte[] bundle, byte[] module) {
        return request(bundle, Map.of("demo.policy.json", module), schema());
    }

    private byte[] resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Test policy resource is missing");
            }
            return input.readAllBytes();
        }
    }
}
