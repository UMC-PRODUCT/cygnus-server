package com.umc.product.maintenance.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.maintenance.adapter.in.web.dto.request.StartMaintenanceRequest;
import com.umc.product.maintenance.domain.MaintenanceScope;
import com.umc.product.member.adapter.out.persistence.MemberSystemRoleJpaRepository;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.MemberSystemRole;
import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.MemberFixture;

@DisplayName("AdminMaintenanceController 통합 테스트")
class AdminMaintenanceControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    MemberSystemRoleJpaRepository memberSystemRoleJpaRepository;

    @Autowired
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    private String superAdminToken;
    private String normalUserToken;

    @BeforeEach
    void setUpAuth() {
        Long superAdminMemberId = setUpSuperAdmin();
        Long normalMemberId = memberFixture.일반("normal-user").getId();
        evictAuthoritySnapshotCacheUseCase.evictByMemberIds(List.of(superAdminMemberId, normalMemberId));

        superAdminToken = "super-admin-token";
        given(jwtTokenProvider.validateAccessToken(superAdminToken)).willReturn(true);
        given(jwtTokenProvider.parseAccessToken(superAdminToken)).willReturn(superAdminMemberId);
        given(jwtTokenProvider.getRolesFromAccessToken(superAdminToken)).willReturn(List.of());

        normalUserToken = "normal-user-token";
        given(jwtTokenProvider.validateAccessToken(normalUserToken)).willReturn(true);
        given(jwtTokenProvider.parseAccessToken(normalUserToken)).willReturn(normalMemberId);
        given(jwtTokenProvider.getRolesFromAccessToken(normalUserToken)).willReturn(List.of());
    }

    @Test
    void SUPER_ADMIN_은_점검을_생성하고_종료할_수_있다() throws Exception {
        Instant now = Instant.now();
        StartMaintenanceRequest body = new StartMaintenanceRequest(
            MaintenanceScope.FULL,
            null,
            now.plusSeconds(30),
            now.plus(Duration.ofHours(2)),
            "정기 점검",
            "더 나은 서비스를 위한 점검입니다"
        );

        String created = mockMvc.perform(post("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.scope").value("FULL"))
            .andExpect(jsonPath("$.result.title").value("정기 점검"))
            .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(created).path("result").path("id").asLong();

        mockMvc.perform(patch("/api/v1/maintenance/admin/" + createdId + "/end")
                .header("Authorization", "Bearer " + superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.forcedEndedAt").isNotEmpty());
    }

    @Test
    void 일반_사용자는_점검_생성_시도시_403() throws Exception {
        Instant now = Instant.now();
        StartMaintenanceRequest body = new StartMaintenanceRequest(
            MaintenanceScope.FULL, null,
            now.plusSeconds(30), now.plus(Duration.ofHours(1)),
            "t", "m"
        );

        mockMvc.perform(post("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + normalUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("MAINTENANCE-0008"));
    }

    @Test
    void 겹치는_시간대의_점검을_만들면_409() throws Exception {
        Instant now = Instant.now();
        StartMaintenanceRequest first = new StartMaintenanceRequest(
            MaintenanceScope.FULL, null,
            now.plusSeconds(30), now.plus(Duration.ofHours(2)),
            "first", "m"
        );
        mockMvc.perform(post("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
            .andExpect(status().isOk());

        StartMaintenanceRequest overlapping = new StartMaintenanceRequest(
            MaintenanceScope.FULL, null,
            now.plus(Duration.ofMinutes(30)),
            now.plus(Duration.ofHours(3)),
            "overlap", "m"
        );
        mockMvc.perform(post("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(overlapping)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("MAINTENANCE-0006"));
    }

    @Test
    void 종료가_시작보다_빠르면_400() throws Exception {
        Instant now = Instant.now();
        StartMaintenanceRequest body = new StartMaintenanceRequest(
            MaintenanceScope.FULL, null,
            now.plus(Duration.ofHours(2)),
            now.plus(Duration.ofMinutes(30)),
            "invalid", "m"
        );

        mockMvc.perform(post("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MAINTENANCE-0003"));
    }

    @Test
    void 윈도우_목록은_SUPER_ADMIN_만_조회_가능() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + superAdminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/maintenance/admin")
                .header("Authorization", "Bearer " + normalUserToken))
            .andExpect(status().isForbidden());
    }

    private Long setUpSuperAdmin() {
        Member member = memberFixture.일반("super-admin-fixture");
        memberSystemRoleJpaRepository.save(MemberSystemRole.create(member.getId(), MemberSystemRoleType.SUPER_ADMIN));
        return member.getId();
    }
}
