package com.umc.product.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;

@WebMvcTest(controllers = StudyGroupQueryController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("스터디 그룹 이름 목록 조회")
class StudyGroupNameQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private GetStudyGroupUseCase getStudyGroupUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(99L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void 준비기수를_지정하여_스터디_이름을_조회한다() throws Exception {
        // given
        given(getStudyGroupUseCase.getStudyGroupNames(99L, 11L))
            .willReturn(List.of(new StudyGroupNameInfo(3L, "웹 트랙 스터디")));

        // when & then
        mockMvc.perform(get("/api/v1/study-groups/names").param("gisuId", "11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.studyGroups[0].name").value("웹 트랙 스터디"));
        then(getStudyGroupUseCase).should().getStudyGroupNames(99L, 11L);
    }

    @Test
    @DisplayName("인증 회원 기준으로 관리 가능한 그룹 이름만 내려준다")
    void returnsManagedGroupNames() throws Exception {
        given(getStudyGroupUseCase.getStudyGroupNames(99L)).willReturn(List.of(
            new StudyGroupNameInfo(1L, "SpringBoot 스터디"),
            new StudyGroupNameInfo(2L, "iOS 스터디")
        ));

        mockMvc.perform(get("/api/v1/study-groups/names"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.studyGroups[0].groupId").value(1L))
            .andExpect(jsonPath("$.result.studyGroups[0].name").value("SpringBoot 스터디"))
            .andExpect(jsonPath("$.result.studyGroups[1].groupId").value(2L));

        then(getStudyGroupUseCase).should().getStudyGroupNames(99L);
    }

    @Test
    @DisplayName("/names 는 /{studyGroupId} 로 잡히지 않는다")
    void namesPathDoesNotCollideWithIdPath() throws Exception {
        given(getStudyGroupUseCase.getStudyGroupNames(anyLong())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/study-groups/names"))
            .andExpect(status().isOk());

        // 상세 조회로 라우팅됐다면 이 UseCase 가 호출됐을 것이다.
        then(getStudyGroupUseCase).should().getStudyGroupNames(99L);
        then(getStudyGroupUseCase).shouldHaveNoMoreInteractions();
    }
}
