package com.umc.product.curriculum.adapter.in.web.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.GetChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.GetOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.GetStudyMemberSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.query.GetWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo.WeeklySubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookPageInfo;
import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.support.RestDocsConfig;

@WebMvcTest(controllers = WorkbookQueryV2Controller.class)
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("WorkbookQueryV2Controller")
class WorkbookQueryV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestDocumentationResultHandler restDocsHandler;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private GetOriginalWorkbookUseCase getOriginalWorkbookUseCase;

    @MockitoBean
    private GetChallengerWorkbookUseCase getChallengerWorkbookUseCase;

    @MockitoBean
    private GetWeeklyBestWorkbookUseCase getWeeklyBestWorkbookUseCase;

    @MockitoBean
    private GetStudyMemberSubmissionUseCase getStudyMemberSubmissionUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(99L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("원본 워크북 조회에 인증 회원 ID를 전달한다")
    void originalWorkbook_passesRequesterMemberId() throws Exception {
        given(getOriginalWorkbookUseCase.getById(1L, 99L)).willReturn(OriginalWorkbookInfo.builder()
            .originalWorkbookId(1L)
            .missions(List.of())
            .build());

        mockMvc.perform(get("/api/v2/curriculums/original-workbooks/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.originalWorkbookId").value(1L))
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("id").description("원본 워크북 ID")
            )));

        then(getOriginalWorkbookUseCase).should().getById(1L, 99L);
    }

    @Test
    @DisplayName("챌린저 워크북 조회에 인증 회원 ID를 전달한다")
    void challengerWorkbook_passesRequesterMemberId() throws Exception {
        given(getChallengerWorkbookUseCase.getById(2L, 99L)).willReturn(ChallengerWorkbookInfo.builder()
            .challengerWorkbookId(2L)
            .requiredMissionIds(Set.of())
            .submissions(List.of())
            .build());

        mockMvc.perform(get("/api/v2/curriculums/challenger-workbooks/{id}", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.challengerWorkbookId").value(2L))
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("id").description("챌린저 워크북 ID")
            )));

        then(getChallengerWorkbookUseCase).should().getById(2L, 99L);
    }

    @Test
    @DisplayName("베스트 조회 size 101은 Bean Validation으로 거부한다")
    void bestWorkbook_size101Rejected() throws Exception {
        mockMvc.perform(get("/api/v2/curriculums/weekly-best-workbooks")
                .param("size", "101"))
            .andExpect(status().isBadRequest());

        then(getWeeklyBestWorkbookUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기수 없이도 베스트를 조회하고 기존 PageResponse shape을 반환한다")
    void bestWorkbook_withoutGisuReturnsPageResponse() throws Exception {
        given(getWeeklyBestWorkbookUseCase.searchBestWorkbooks(any()))
            .willReturn(new WeeklyBestWorkbookPageInfo(List.of(), 0, 20, 0, 0, false, false));

        mockMvc.perform(get("/api/v2/curriculums/weekly-best-workbooks")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content").isArray())
            .andExpect(jsonPath("$.result.page").value(0))
            .andExpect(jsonPath("$.result.size").value(20))
            .andExpect(jsonPath("$.result.totalElements").value(0))
            .andExpect(jsonPath("$.result.totalPages").value(0))
            .andDo(restDocsHandler.document(queryParameters(
                parameterWithName("gisuId").description("기수 ID").optional(),
                parameterWithName("schoolIds").description("학교 ID 목록").optional(),
                parameterWithName("parts").description("파트 목록").optional(),
                parameterWithName("weekNos").description("주차 번호 목록").optional(),
                parameterWithName("studyGroupIds").description("스터디 그룹 ID 목록").optional(),
                parameterWithName("page").description("0부터 시작하는 페이지 번호. 기본값 0").optional(),
                parameterWithName("size").description("페이지 크기. 기본값 20, 최대 100").optional()
            )));
    }

    @Test
    @DisplayName("스터디원 제출 현황을 커서 응답으로 반환하고 미배포 인원도 포함한다")
    void studyMemberSubmissions_returnsCursorResponse() throws Exception {
        given(getStudyMemberSubmissionUseCase.getStudyMemberSubmissions(any())).willReturn(List.of(
            StudyMemberSubmissionInfo.builder()
                .studyGroupMemberId(51L)
                .memberId(100L)
                .memberName("김통과")
                .studyGroupId(10L)
                .studyGroupName("SpringBoot 스터디")
                .part(ChallengerPart.SPRINGBOOT)
                .weeks(List.of(WeeklySubmissionInfo.builder()
                    .weekNo(3L)
                    .weeklyCurriculumId(20L)
                    .weeklyCurriculumTitle("3주차")
                    .challengerWorkbookId(null)
                    .status(ChallengerWorkbookStatus.NOT_SUBMITTED)
                    .isBest(false)
                    .build()))
                .build()
        ));

        mockMvc.perform(get("/api/v2/curriculums/workbook-submissions")
                .param("studyGroupId", "10")
                .param("weekNos", "3")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].studyGroupMemberId").value(51L))
            .andExpect(jsonPath("$.result.content[0].weeks[0].weeklyCurriculumTitle").value("3주차"))
            .andExpect(jsonPath("$.result.content[0].weeks[0].challengerWorkbookId").doesNotExist())
            .andExpect(jsonPath("$.result.content[0].weeks[0].status").value("NOT_SUBMITTED"))
            .andExpect(jsonPath("$.result.hasNext").value(false))
            .andDo(restDocsHandler.document(queryParameters(
                parameterWithName("studyGroupId").description("스터디 그룹 ID. 생략 시 조회 가능한 전체 그룹").optional(),
                parameterWithName("weekNos").description("주차 번호 목록. 생략 시 전체 주차").optional(),
                parameterWithName("cursor").description("직전 페이지 마지막 studyGroupMemberId").optional(),
                parameterWithName("size").description("페이지 크기. 기본값 20, 최대 100").optional()
            )));
    }

    @Test
    @DisplayName("제출 현황 size 101은 Bean Validation으로 거부한다")
    void studyMemberSubmissions_size101Rejected() throws Exception {
        mockMvc.perform(get("/api/v2/curriculums/workbook-submissions")
                .param("size", "101"))
            .andExpect(status().isBadRequest());

        then(getStudyMemberSubmissionUseCase).shouldHaveNoInteractions();
    }
}
