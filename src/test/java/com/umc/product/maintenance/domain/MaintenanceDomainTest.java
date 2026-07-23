package com.umc.product.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaintenanceDomainTest {

    @Test
    @DisplayName("URI 가 challenger 경로면 CHALLENGER 매칭")
    void challenger_uri_매칭() {
        assertThat(MaintenanceDomain.fromUri("/api/v1/challenger/me"))
            .contains(MaintenanceDomain.CHALLENGER);
        assertThat(MaintenanceDomain.fromUri("/api/v1/challenger-record/123"))
            .contains(MaintenanceDomain.CHALLENGER);
    }

    @Test
    @DisplayName("URI 가 어느 도메인에도 속하지 않으면 empty")
    void 매칭되지_않는_URI() {
        assertThat(MaintenanceDomain.fromUri("/api/v1/system/status"))
            .isEqualTo(Optional.empty());
        assertThat(MaintenanceDomain.fromUri("/random/path"))
            .isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("study-groups 는 ORGANIZATION 으로 매칭")
    void study_groups_매칭() {
        assertThat(MaintenanceDomain.fromUri("/api/v1/study-groups/1"))
            .contains(MaintenanceDomain.ORGANIZATION);
    }

    @Test
    @DisplayName("project 와 projects 모두 PROJECT 로 매칭")
    void project_매칭() {
        assertThat(MaintenanceDomain.fromUri("/api/v1/projects/1"))
            .contains(MaintenanceDomain.PROJECT);
        assertThat(MaintenanceDomain.fromUri("/api/v1/project/matching-rounds"))
            .contains(MaintenanceDomain.PROJECT);
    }

    @Test
    @DisplayName("community 와 posts URI 모두 COMMUNITY 로 매칭")
    void community_매칭() {
        assertThat(MaintenanceDomain.fromUri("/api/v1/community/threads/1"))
            .contains(MaintenanceDomain.COMMUNITY);
        assertThat(MaintenanceDomain.fromUri("/api/v1/posts/1"))
            .contains(MaintenanceDomain.COMMUNITY);
    }
}
