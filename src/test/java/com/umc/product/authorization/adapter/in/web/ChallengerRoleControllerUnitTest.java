package com.umc.product.authorization.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.adapter.in.web.dto.request.CreateChallengerRoleRequest;
import com.umc.product.authorization.adapter.in.web.dto.response.ChallengerRoleResponse;
import com.umc.product.authorization.adapter.in.web.dto.response.CreateChallengerRoleResponse;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.DeleteChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRoleController 단위 계약")
class ChallengerRoleControllerUnitTest {

    @Mock
    ManageChallengerRoleUseCase manageChallengerRoleUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    ChallengerRoleController sut;

    @BeforeEach
    void setUp() {
        sut = new ChallengerRoleController(
            manageChallengerRoleUseCase,
            getChallengerRoleUseCase,
            getGisuUseCase
        );
    }

    @Test
    @DisplayName("생성 요청의 모든 필드를 command로 전달하고 생성 ID를 응답한다")
    void 역할을_생성한다() {
        CreateChallengerRoleRequest request = new CreateChallengerRoleRequest(
            10L,
            ChallengerRoleType.SCHOOL_PART_LEADER,
            20L,
            ChallengerPart.SPRINGBOOT,
            30L
        );
        given(manageChallengerRoleUseCase.createChallengerRole(org.mockito.ArgumentMatchers.any()))
            .willReturn(40L);

        CreateChallengerRoleResponse result = sut.createChallengerRole(request);

        assertThat(result.challengerRoleId()).isEqualTo(40L);
        ArgumentCaptor<CreateChallengerRoleCommand> captor =
            ArgumentCaptor.forClass(CreateChallengerRoleCommand.class);
        then(manageChallengerRoleUseCase).should().createChallengerRole(captor.capture());
        assertThat(captor.getValue()).satisfies(command -> {
            assertThat(command.challengerId()).isEqualTo(10L);
            assertThat(command.roleType()).isEqualTo(ChallengerRoleType.SCHOOL_PART_LEADER);
            assertThat(command.organizationId()).isEqualTo(20L);
            assertThat(command.responsiblePart()).isEqualTo(ChallengerPart.SPRINGBOOT);
            assertThat(command.gisuId()).isEqualTo(30L);
        });
    }

    @Test
    @DisplayName("조회 응답은 역할과 기수 generation을 함께 노출한다")
    @SuppressWarnings("removal")
    void 역할과_기수를_조회한다() {
        ChallengerRoleInfo info = ChallengerRoleInfo.builder()
            .id(40L)
            .challengerId(10L)
            .roleType(ChallengerRoleType.SCHOOL_PART_LEADER)
            .organizationType(OrganizationType.SCHOOL)
            .organizationId(20L)
            .responsiblePart(ChallengerPart.SPRINGBOOT)
            .gisuId(30L)
            .build();
        given(getChallengerRoleUseCase.getById(40L)).willReturn(info);
        given(getGisuUseCase.getById(30L)).willReturn(new GisuInfo(30L, 12L, null, null, true));

        ChallengerRoleResponse result = sut.getChallengerRole(40L);

        assertThat(result).satisfies(response -> {
            assertThat(response.challengerRoleId()).isEqualTo(40L);
            assertThat(response.challengerId()).isEqualTo(10L);
            assertThat(response.organizationType()).isEqualTo(OrganizationType.SCHOOL);
            assertThat(response.gisuId()).isEqualTo(30L);
            assertThat(response.gisu()).isEqualTo(12L);
        });
    }

    @Test
    @DisplayName("삭제 ID를 command로 변환해 관리 UseCase에 전달한다")
    void 역할을_삭제한다() {
        sut.deleteChallengerRole(40L);

        then(manageChallengerRoleUseCase).should().deleteChallengerRole(
            DeleteChallengerRoleCommand.builder().challengerRoleId(40L).build()
        );
    }
}
