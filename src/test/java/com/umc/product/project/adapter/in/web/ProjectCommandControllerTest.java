package com.umc.product.project.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
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
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.dto.request.AbortProjectRequest;
import com.umc.product.project.adapter.in.web.dto.request.ChangeProjectMemberStatusRequest;
import com.umc.product.project.adapter.in.web.dto.request.CompleteProjectsRequest;
import com.umc.product.project.application.port.in.command.AbortProjectUseCase;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.ChangeProjectMemberStatusUseCase;
import com.umc.product.project.application.port.in.command.CompleteProjectsUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.RemoveProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.TransferProjectOwnershipUseCase;
import com.umc.product.project.application.port.in.command.UpdatePartQuotasUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

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
    @MockitoBean
    CompleteProjectsUseCase completeProjectsUseCase;

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
    void POST_프로젝트_완료_200() throws Exception {
        CompleteProjectsRequest request = new CompleteProjectsRequest(List.of(1L, 2L, 3L));

        mockMvc.perform(post("/api/v1/projects/complete")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        then(completeProjectsUseCase).should().complete(any());
    }

    @Test
    void POST_프로젝트_완료_projectIds_비어있으면_400() throws Exception {
        CompleteProjectsRequest request = new CompleteProjectsRequest(List.of());

        mockMvc.perform(post("/api/v1/projects/complete")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(completeProjectsUseCase).should(never()).complete(any());
    }

    @Test
    void POST_프로젝트_완료_body_누락이면_400() throws Exception {
        mockMvc.perform(post("/api/v1/projects/complete")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(completeProjectsUseCase).should(never()).complete(any());
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
}
