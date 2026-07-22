package com.umc.product.project.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.dto.request.AbortProjectRequest;
import com.umc.product.project.adapter.in.web.dto.request.AddProjectMemberRequest;
import com.umc.product.project.adapter.in.web.dto.request.ChangeProjectMemberStatusRequest;
import com.umc.product.project.adapter.in.web.dto.request.CreateDraftProjectRequest;
import com.umc.product.project.adapter.in.web.dto.request.TransferProjectOwnershipRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdatePartQuotasRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateProjectRequest;
import com.umc.product.project.application.port.in.command.AbortProjectUseCase;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.ChangeProjectMemberStatusUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.RemoveProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.TransferProjectOwnershipUseCase;
import com.umc.product.project.application.port.in.command.UpdatePartQuotasUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@WebMvcTest(controllers = ProjectCommandController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectCommandControllerTest {

    private static final Long TEST_MEMBER_ID = 99L;
    private static final Long PROJECT_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProjectCommandController controller;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CreateDraftProjectUseCase createDraftProjectUseCase;
    @MockitoBean
    UpdateProjectUseCase updateProjectUseCase;
    @MockitoBean
    SubmitProjectUseCase submitProjectUseCase;
    @MockitoBean
    TransferProjectOwnershipUseCase transferProjectOwnershipUseCase;
    @MockitoBean
    AddProjectMemberUseCase addProjectMemberUseCase;
    @MockitoBean
    RemoveProjectMemberUseCase removeProjectMemberUseCase;
    @MockitoBean
    ChangeProjectMemberStatusUseCase changeProjectMemberStatusUseCase;
    @MockitoBean
    UpdatePartQuotasUseCase updatePartQuotasUseCase;
    @MockitoBean
    PublishProjectUseCase publishProjectUseCase;
    @MockitoBean
    DeleteProjectUseCase deleteProjectUseCase;
    @MockitoBean
    AbortProjectUseCase abortProjectUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(TEST_MEMBER_ID)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void DELETE_프로젝트_삭제_204() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/" + PROJECT_ID))
            .andExpect(status().isOk());

        then(deleteProjectUseCase).should().delete(any());
    }

    @Test
    void POST_프로젝트_중단_204() throws Exception {
        AbortProjectRequest request = new AbortProjectRequest("팀이 데모데이 불참으로 와해됨");

        mockMvc.perform(post("/api/v1/projects/" + PROJECT_ID + "/abort")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        then(abortProjectUseCase).should().abort(any());
    }

    @Test
    void POST_프로젝트_중단_reason_누락이면_400() throws Exception {
        AbortProjectRequest request = new AbortProjectRequest("  ");

        mockMvc.perform(post("/api/v1/projects/" + PROJECT_ID + "/abort")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(abortProjectUseCase).should(never()).abort(any());
    }

    @Test
    void POST_프로젝트_중단_body_누락이면_400() throws Exception {
        mockMvc.perform(post("/api/v1/projects/" + PROJECT_ID + "/abort")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(abortProjectUseCase).should(never()).abort(any());
    }

    @Test
    void DELETE_프로젝트_팀원_제거_hard_delete_200() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/" + PROJECT_ID + "/members/200"))
            .andExpect(status().isOk());

        then(removeProjectMemberUseCase).should().remove(any());
    }

    @Test
    void PATCH_프로젝트_팀원_상태변경_soft_delete_200() throws Exception {
        ChangeProjectMemberStatusRequest request =
            new ChangeProjectMemberStatusRequest(ProjectMemberStatus.DISMISSED, "팀 적합도 부족");

        mockMvc.perform(patch("/api/v1/projects/" + PROJECT_ID + "/members/200/status")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        then(changeProjectMemberStatusUseCase).should().changeStatus(any());
    }

    @Test
    void PATCH_프로젝트_팀원_상태변경_reason_누락이면_400() throws Exception {
        ChangeProjectMemberStatusRequest request =
            new ChangeProjectMemberStatusRequest(ProjectMemberStatus.DISMISSED, "  ");

        mockMvc.perform(patch("/api/v1/projects/" + PROJECT_ID + "/members/200/status")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(changeProjectMemberStatusUseCase).should(never()).changeStatus(any());
    }

    @Test
    void PATCH_프로젝트_팀원_상태변경_status_누락이면_400() throws Exception {
        ChangeProjectMemberStatusRequest request =
            new ChangeProjectMemberStatusRequest(null, "사유");

        mockMvc.perform(patch("/api/v1/projects/" + PROJECT_ID + "/members/200/status")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(changeProjectMemberStatusUseCase).should(never()).changeStatus(any());
    }

    @Test
    void 프로젝트_생성_수정_제출_소유권_팀원_공개_정원_흐름을_위임한다() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(TEST_MEMBER_ID).build();
        given(createDraftProjectUseCase.create(any())).willReturn(PROJECT_ID);
        given(updateProjectUseCase.update(any())).willReturn(ProjectStatus.DRAFT);
        given(transferProjectOwnershipUseCase.transfer(any())).willReturn(ProjectStatus.DRAFT);
        given(addProjectMemberUseCase.add(any())).willReturn(300L);
        given(publishProjectUseCase.publish(any())).willReturn(ProjectStatus.IN_PROGRESS);

        assertThat(controller.createDraft(principal, new CreateDraftProjectRequest(1L, null)).projectId())
            .isEqualTo(PROJECT_ID);
        assertThat(controller.update(principal, PROJECT_ID,
            new UpdateProjectRequest("이름", "설명", null, null, null)).status())
            .isEqualTo(ProjectStatus.DRAFT);
        assertThat(controller.submit(PROJECT_ID).status()).isEqualTo(ProjectStatus.PENDING_REVIEW);
        assertThat(controller.transferOwnership(PROJECT_ID,
            new TransferProjectOwnershipRequest(200L, "양도")).status()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(controller.addMember(principal, PROJECT_ID,
            new AddProjectMemberRequest(200L, ChallengerPart.WEB))).isEqualTo(300L);
        assertThat(controller.publish(principal, PROJECT_ID).status()).isEqualTo(ProjectStatus.IN_PROGRESS);
        controller.updatePartQuotas(principal, PROJECT_ID,
            new UpdatePartQuotasRequest(List.of(new UpdatePartQuotasRequest.Entry(ChallengerPart.WEB, 2L))));

        then(submitProjectUseCase).should().submit(any());
        then(updatePartQuotasUseCase).should().update(any());
    }
}
