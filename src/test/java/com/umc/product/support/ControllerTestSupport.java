package com.umc.product.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notice.adapter.in.web.NoticeCommandController;
import com.umc.product.notice.adapter.in.web.NoticeContentController;
import com.umc.product.notice.adapter.in.web.NoticeQueryController;
import com.umc.product.notice.adapter.in.web.assembler.NoticeViewerInfoAssembler;
import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.ManageNoticeReadUseCase;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.query.GetNoticeUseCase;
import com.umc.product.organization.adapter.in.web.ChapterCommandController;
import com.umc.product.organization.adapter.in.web.ChapterQueryController;
import com.umc.product.organization.adapter.in.web.GisuCommandController;
import com.umc.product.organization.adapter.in.web.GisuQueryController;
import com.umc.product.organization.adapter.in.web.SchoolCommandController;
import com.umc.product.organization.adapter.in.web.SchoolQueryController;
import com.umc.product.organization.adapter.in.web.UmcProductChapterCommandController;
import com.umc.product.organization.adapter.in.web.UmcProductChapterQueryController;
import com.umc.product.organization.adapter.in.web.UmcProductMemberCommandController;
import com.umc.product.organization.adapter.in.web.UmcProductMemberQueryController;
import com.umc.product.organization.adapter.in.web.UmcProductOrganizationChartQueryController;
import com.umc.product.organization.adapter.in.web.UmcProductSquadCommandController;
import com.umc.product.organization.adapter.in.web.UmcProductSquadQueryController;
import com.umc.product.organization.application.port.in.command.ManageChapterUseCase;
import com.umc.product.organization.application.port.in.command.ManageGisuUseCase;
import com.umc.product.organization.application.port.in.command.ManageSchoolUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductChapterUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductSquadUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductOrganizationChartUseCase;
import com.umc.product.organization.application.port.in.query.GetUmcProductSquadUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@WebMvcTest(controllers = {
    SchoolCommandController.class,
    SchoolQueryController.class,
    ChapterCommandController.class,
    ChapterQueryController.class,
    GisuCommandController.class,
    GisuQueryController.class,
    UmcProductChapterCommandController.class,
    UmcProductChapterQueryController.class,
    UmcProductMemberCommandController.class,
    UmcProductMemberQueryController.class,
    UmcProductSquadCommandController.class,
    UmcProductSquadQueryController.class,
    UmcProductOrganizationChartQueryController.class,
    NoticeCommandController.class,
    NoticeQueryController.class,
    NoticeQueryController.class,
    NoticeContentController.class,
})
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
public class ControllerTestSupport {
    // 컨트롤러 테스트에 필요한 공통 설정과 의존성을 제공한다.

    protected static final Long TEST_MEMBER_ID = 1L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected ManageSchoolUseCase manageSchoolUseCase;

    @MockitoBean
    protected GetSchoolUseCase getSchoolUseCase;

    @MockitoBean
    protected ManageChapterUseCase manageChapterUseCase;

    @MockitoBean
    protected GetChapterUseCase getChapterUseCase;

    @MockitoBean
    protected ManageGisuUseCase manageGisuUseCase;

    @MockitoBean
    protected GetGisuUseCase getGisuUseCase;

    @MockitoBean
    protected ManageUmcProductChapterUseCase manageUmcProductChapterUseCase;

    @MockitoBean
    protected GetUmcProductChapterUseCase getUmcProductChapterUseCase;

    @MockitoBean
    protected ManageUmcProductMemberUseCase manageUmcProductMemberUseCase;

    @MockitoBean
    protected GetUmcProductMemberUseCase getUmcProductMemberUseCase;

    @MockitoBean
    protected ManageUmcProductSquadUseCase manageUmcProductSquadUseCase;

    @MockitoBean
    protected GetUmcProductSquadUseCase getUmcProductSquadUseCase;

    @MockitoBean
    protected GetUmcProductOrganizationChartUseCase getUmcProductOrganizationChartUseCase;

    @MockitoBean
    protected GetFileUseCase getFileUseCase;

    @MockitoBean
    protected ManageNoticeUseCase manageNoticeUseCase;

    @MockitoBean
    protected ManageNoticeReadUseCase manageNoticeReadUseCase;

    @MockitoBean
    protected ManageNoticeContentUseCase manageNoticeContentUseCase;

    @MockitoBean
    protected GetNoticeUseCase getNoticeUseCase;

    @MockitoBean
    protected NoticeViewerInfoAssembler noticeViewerInfoAssembler;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(TEST_MEMBER_ID)
            .build();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
