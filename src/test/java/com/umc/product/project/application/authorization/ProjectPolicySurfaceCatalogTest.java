package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class ProjectPolicySurfaceCatalogTest {

    @Test
    @DisplayName("실제 Project 호출면과 정책 카탈로그가 양방향으로 정확히 일치한다")
    void catalogExactlyMatchesDiscoveredSurfaces() {
        Set<ProjectPolicySurfaceIdentity> discovered =
            ProjectPolicyRuntimeSurfaceDiscovery.discoverAnnotatedSurfaces(new StandardEnvironment());

        assertThat(discovered).hasSize(46);
        assertThat(discovered.stream().filter(surface -> surface.id().startsWith("rest:"))).hasSize(37);
        assertThat(discovered.stream().filter(surface -> surface.id().startsWith("graphql:"))).hasSize(8);
        assertThat(discovered.stream().filter(surface -> surface.id().startsWith("scheduler:"))).hasSize(1);
        assertThat(ProjectPolicySurfaceCatalog.identities()).containsExactlyInAnyOrderElementsOf(discovered);
    }

    @Test
    @DisplayName("누락되거나 임의로 추가된 호출면은 exact-set 검증을 통과하지 못한다")
    void rejectsMissingAndDummySurfaceMutations() {
        Set<ProjectPolicySurfaceIdentity> exact = ProjectPolicySurfaceCatalog.identities();
        ProjectPolicySurfaceIdentity existing = exact.iterator().next();

        Set<ProjectPolicySurfaceIdentity> missing = new LinkedHashSet<>(exact);
        missing.remove(existing);
        Set<ProjectPolicySurfaceIdentity> dummy = new LinkedHashSet<>(exact);
        dummy.add(new ProjectPolicySurfaceIdentity("rest:GET /dummy", "example.DummyController#dummy"));

        assertThatThrownBy(() -> ProjectPolicySurfaceCatalog.assertExactIdentities(Set.copyOf(missing)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing=")
            .hasMessageContaining(existing.id());
        assertThatThrownBy(() -> ProjectPolicySurfaceCatalog.assertExactIdentities(Set.copyOf(dummy)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unexpected=")
            .hasMessageContaining("rest:GET /dummy");
    }

}
