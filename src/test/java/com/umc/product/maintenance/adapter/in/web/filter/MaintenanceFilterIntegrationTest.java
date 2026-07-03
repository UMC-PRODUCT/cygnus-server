package com.umc.product.maintenance.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.maintenance.application.port.out.SaveMaintenanceWindowPort;
import com.umc.product.maintenance.application.service.MaintenanceStateHolder;
import com.umc.product.maintenance.domain.MaintenanceDomain;
import com.umc.product.maintenance.domain.MaintenanceScope;
import com.umc.product.maintenance.domain.MaintenanceWindow;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.MemberFixture;

@DisplayName("MaintenanceFilter 통합 테스트")
class MaintenanceFilterIntegrationTest extends IntegrationTestSupport {

    @Autowired
    SaveMaintenanceWindowPort saveMaintenanceWindowPort;

    @Autowired
    MaintenanceStateHolder maintenanceStateHolder;

    @Autowired
    SaveChallengerRolePort saveChallengerRolePort;

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    ChallengerFixture challengerFixture;

    @Autowired
    GisuFixture gisuFixture;

    @Test
    void 점검중이_아닐_때_시스템_상태_조회는_정상_200() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.inMaintenance").value(false));
    }

    @Test
    void FULL_점검중에도_시스템_상태_조회는_200() throws Exception {
        saveActiveFullWindow();

        mockMvc.perform(get("/api/v1/system/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.inMaintenance").value(true));
    }

    @Test
    void FULL_점검중_비인증_요청은_503_과_점검_정보_반환() throws Exception {
        saveActiveFullWindow();

        mockMvc.perform(get("/api/v1/challenger/me"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("MAINTENANCE-0001"))
            .andExpect(jsonPath("$.result.scope").value("FULL"))
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void FULL_점검중_SUPER_ADMIN_토큰이면_필터를_통과한다() throws Exception {
        saveActiveFullWindow();
        Long superAdminMemberId = setUpSuperAdmin();

        String token = "super-admin-token";
        given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.parseAccessToken(token)).willReturn(superAdminMemberId);
        given(jwtTokenProvider.getRolesFromAccessToken(token)).willReturn(java.util.List.of());

        // 필터를 통과한 뒤의 동작은 컨트롤러 존재 여부에 따라 다르므로
        // "점검 503 이 아니다" 만 검증한다.
        int actualStatus = mockMvc.perform(get("/api/v1/challenger/me")
                .header("Authorization", "Bearer " + token))
            .andReturn().getResponse().getStatus();

        assertThat(actualStatus).isNotEqualTo(503);
    }

    @Test
    void PER_DOMAIN_점검은_지정되지_않은_도메인_요청을_차단하지_않는다() throws Exception {
        saveActivePerDomainWindow(EnumSet.of(MaintenanceDomain.NOTICE));

        // NOTICE 만 점검 대상. /api/v1/challenger/me 는 CHALLENGER 도메인 → 통과
        int actualStatus = mockMvc.perform(get("/api/v1/challenger/me"))
            .andReturn().getResponse().getStatus();

        assertThat(actualStatus).isNotEqualTo(503);
    }

    @Test
    void PER_DOMAIN_점검은_지정된_도메인_요청을_503으로_차단() throws Exception {
        saveActivePerDomainWindow(EnumSet.of(MaintenanceDomain.NOTICE));

        mockMvc.perform(get("/api/v1/notices/1"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("MAINTENANCE-0001"))
            .andExpect(jsonPath("$.result.scope").value("PER_DOMAIN"))
            .andExpect(jsonPath("$.result.targetDomains[0]").value("NOTICE"));
    }

    @Test
    void 어드민_점검_관리_경로는_점검중에도_항상_통과() throws Exception {
        saveActiveFullWindow();

        // 비인증 호출이라 권한 검증으로 인해 403/401 등 다른 코드가 반환되어도
        // 필터에서 차단되지 않았음을 확인한다.
        int actualStatus = mockMvc.perform(get("/api/v1/maintenance/admin"))
            .andReturn().getResponse().getStatus();

        assertThat(actualStatus).isNotEqualTo(503);
    }

    @Test
    void Actuator_경로는_점검중에도_항상_통과() throws Exception {
        saveActiveFullWindow();

        int actualStatus = mockMvc.perform(get("/actuator/health"))
            .andReturn().getResponse().getStatus();

        assertThat(actualStatus).isNotEqualTo(503);
    }

    @Test
    @DisplayName("FULL 점검 중에도 약관 조회는 점검 필터를 통과한다")
    void fullMaintenanceAllowsTermLookup() throws Exception {
        saveActiveFullWindow();

        mockMvc.perform(get("/api/v1/terms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.terms").isArray());
    }

    private void saveActiveFullWindow() {
        Instant now = Instant.now();
        MaintenanceWindow window = MaintenanceWindow.of(
            MaintenanceScope.FULL,
            null,
            now.minusSeconds(30),
            now.plus(Duration.ofHours(1)),
            "정기 점검",
            "잠시만 기다려주세요",
            999L,
            now
        );
        saveMaintenanceWindowPort.save(window);
        maintenanceStateHolder.refresh();
    }

    private void saveActivePerDomainWindow(EnumSet<MaintenanceDomain> domains) {
        Instant now = Instant.now();
        MaintenanceWindow window = MaintenanceWindow.of(
            MaintenanceScope.PER_DOMAIN,
            domains,
            now.minusSeconds(30),
            now.plus(Duration.ofHours(1)),
            "부분 점검",
            "특정 기능만 점검 중입니다",
            999L,
            now
        );
        saveMaintenanceWindowPort.save(window);
        maintenanceStateHolder.refresh();
    }

    private Long setUpSuperAdmin() {
        Gisu gisu = gisuFixture.비활성_기수(99L);
        Member member = memberFixture.일반("super-admin-fixture");
        Challenger challenger = challengerFixture.챌린저(member.getId(), ChallengerPart.WEB, gisu.getId());
        saveChallengerRolePort.save(ChallengerRole.create(
            challenger.getId(),
            ChallengerRoleType.SUPER_ADMIN,
            null,
            null,
            gisu.getId()
        ));
        return member.getId();
    }
}
