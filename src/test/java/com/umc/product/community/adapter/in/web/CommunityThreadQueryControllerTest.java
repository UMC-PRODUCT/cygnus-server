package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.query.thread.BrowseCommunityThreadsUseCase;
import com.umc.product.community.application.port.in.query.thread.GetPublicCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.query.thread.SearchCommunityThreadInvitableUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.BrowseThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitableInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitablePageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadLastMessageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListFilter;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberPageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadSummaryInfo;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageHistoryUseCase;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageHistoryQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMentionInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessagePageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageReplyInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = CommunityThreadQueryController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community thread 조회 REST adapter")
class CommunityThreadQueryControllerTest {

    private static final Long REQUESTER_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BrowseCommunityThreadsUseCase browseThreadsUseCase;

    @MockitoBean
    private GetPublicCommunityThreadDetailUseCase getPublicThreadDetailUseCase;

    @MockitoBean
    private ListCommunityThreadMembersUseCase listThreadMembersUseCase;

    @MockitoBean
    private SearchCommunityThreadInvitableUseCase searchThreadInvitableUseCase;

    @MockitoBean
    private GetCommunityThreadMessageHistoryUseCase getMessageHistoryUseCase;

    @BeforeEach
    void setUpCurrentMember() {
        MemberPrincipal principal = new MemberPrincipal(REQUESTER_ID);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("thread 목록 기본 query와 숫자 문자열 응답을 보존한다")
    void listThreads_usesDefaultsAndSerializesNumericValuesAsStrings() throws Exception {
        given(browseThreadsUseCase.browseThreads(any())).willReturn(new ThreadListInfo(
            List.of(summary(42L, true)), List.of(summary(43L, false)), 20, 21L
        ));

        mockMvc.perform(get("/api/v1/community/threads"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.pinned[0].threadId").value("42"))
            .andExpect(jsonPath("$.result.pinned[0].memberCount").value("3"))
            .andExpect(jsonPath("$.result.pinned[0].unreadCount").value("2"))
            .andExpect(jsonPath("$.result.pinned[0].isPinned").value(true))
            .andExpect(jsonPath("$.result.threads[0].threadId").value("43"))
            .andExpect(jsonPath("$.result.nextOffset").value("20"))
            .andExpect(jsonPath("$.result.total").value("21"));

        ArgumentCaptor<BrowseThreadsQuery> captor = ArgumentCaptor.forClass(BrowseThreadsQuery.class);
        then(browseThreadsUseCase).should().browseThreads(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().filter().name()).isEqualTo("ALL");
        assertThat(captor.getValue().offset()).isZero();
        assertThat(captor.getValue().limit()).isEqualTo(20);
    }

    @ParameterizedTest
    @CsvSource({
        "all, ALL",
        "unread, UNREAD",
        "STUDY, STUDY",
        "QNA, QNA",
        "PROJECT, PROJECT",
        "FREE, FREE"
    })
    @DisplayName("문서화된 여섯 filter 값을 명시적으로 application enum으로 변환한다")
    void listThreads_parsesDocumentedFilterValues(String rawFilter, ThreadListFilter expectedFilter)
        throws Exception {
        given(browseThreadsUseCase.browseThreads(any())).willReturn(
            new ThreadListInfo(List.of(), List.of(), null, 0L)
        );

        mockMvc.perform(get("/api/v1/community/threads").param("filter", rawFilter))
            .andExpect(status().isOk());

        ArgumentCaptor<BrowseThreadsQuery> captor = ArgumentCaptor.forClass(BrowseThreadsQuery.class);
        then(browseThreadsUseCase).should().browseThreads(captor.capture());
        assertThat(captor.getValue().filter()).isEqualTo(expectedFilter);
    }

    @Test
    @DisplayName("thread 상세의 nullable 값과 boolean은 JSON 원시 타입을 유지한다")
    void getThread_keepsNullAndBooleanJsonTypes() throws Exception {
        given(getPublicThreadDetailUseCase.getPublicThread(any())).willReturn(new ThreadDetailInfo(
            42L, "스터디", null, CommunityThreadCategory.STUDY, "📚",
            3L, 0L, 100, false, true, true, CommunityThreadMemberRole.OWNER,
            null, 99L, NOW, NOW, "/api/v1/community/threads/42", null
        ));

        mockMvc.perform(get("/api/v1/community/threads/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.threadId").value("42"))
            .andExpect(jsonPath("$.result.description").doesNotExist())
            .andExpect(jsonPath("$.result.lastMessage").doesNotExist())
            .andExpect(jsonPath("$.result.isMuted").value(true))
            .andExpect(jsonPath("$.result.createdBy").value("99"));
    }

    @Test
    @DisplayName("member 페이지의 ID, offset, total을 문자열로 반환한다")
    void listMembers_serializesNumericContract() throws Exception {
        given(listThreadMembersUseCase.listMembers(any())).willReturn(new ThreadMemberPageInfo(
            List.of(new ThreadMemberInfo(
                7L, "하늘", ChallengerPart.SPRINGBOOT, 8L,
                CommunityThreadMemberRole.ADMIN, NOW, CommunityThreadMemberState.ACTIVE
            )), 20, 21L
        ));

        mockMvc.perform(get("/api/v1/community/threads/42/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].memberId").value("7"))
            .andExpect(jsonPath("$.result.nextOffset").value("20"))
            .andExpect(jsonPath("$.result.total").value("21"));
    }

    @Test
    @DisplayName("invitable 페이지의 member와 Challenger ID를 문자열로 반환한다")
    void searchInvitable_serializesNumericContract() throws Exception {
        given(searchThreadInvitableUseCase.searchInvitable(any())).willReturn(new ThreadInvitablePageInfo(
            List.of(new ThreadInvitableInfo(8L, 88L, "구름", ChallengerPart.IOS, 8L)),
            null, 1L
        ));

        mockMvc.perform(get("/api/v1/community/threads/42/invitable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].memberId").value("8"))
            .andExpect(jsonPath("$.result.items[0].challengerId").value("88"))
            .andExpect(jsonPath("$.result.nextOffset").doesNotExist())
            .andExpect(jsonPath("$.result.total").value("1"));
    }

    @Test
    @DisplayName("Challenger 이력이 없는 초대 후보는 관련 필드를 null로 반환한다")
    void searchInvitable_serializesMissingChallengerAsNull() throws Exception {
        given(searchThreadInvitableUseCase.searchInvitable(any())).willReturn(new ThreadInvitablePageInfo(
            List.of(new ThreadInvitableInfo(8L, null, "구름", null, null)),
            null, 1L
        ));

        mockMvc.perform(get("/api/v1/community/threads/42/invitable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].memberId").value("8"))
            .andExpect(jsonPath("$.result.items[0].challengerId").value(nullValue()))
            .andExpect(jsonPath("$.result.items[0].part").value(nullValue()))
            .andExpect(jsonPath("$.result.items[0].generation").value(nullValue()));
    }

    @Test
    @DisplayName("history cursor를 exclusive before query로 전달하고 nested 숫자를 문자열로 반환한다")
    void getMessageHistory_mapsCursorAndNestedNumericValues() throws Exception {
        CommunityThreadMessageInfo message = new CommunityThreadMessageInfo(
            500L, 42L, 7L, "하늘", "안녕하세요", CommunityThreadMessageType.TEXT,
            CommunityThreadMessageStatus.SENT, List.of("file-key"),
            List.of(new CommunityThreadMessageMentionInfo(8L, "구름")),
            new CommunityThreadMessageReplyInfo(499L, "구름", "이전 메시지"),
            List.of(new CommunityThreadReactionInfo("👍", 2L, true)), null,
            NOW, null, null
        );
        given(getMessageHistoryUseCase.getHistory(any())).willReturn(
            new CommunityThreadMessagePageInfo(List.of(message), true, 499L)
        );

        mockMvc.perform(get("/api/v1/community/threads/42/messages")
                .param("before", "500")
                .param("limit", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.messages[0].messageId").value("500"))
            .andExpect(jsonPath("$.result.messages[0].mentions[0].memberId").value("8"))
            .andExpect(jsonPath("$.result.messages[0].replyTo.messageId").value("499"))
            .andExpect(jsonPath("$.result.messages[0].reactions[0].count").value("2"))
            .andExpect(jsonPath("$.result.hasMore").value(true))
            .andExpect(jsonPath("$.result.nextBefore").value("499"));

        ArgumentCaptor<CommunityThreadMessageHistoryQuery> captor =
            ArgumentCaptor.forClass(CommunityThreadMessageHistoryQuery.class);
        then(getMessageHistoryUseCase).should().getHistory(captor.capture());
        assertThat(captor.getValue().beforeMessageId()).isEqualTo(500L);
    }

    private ThreadSummaryInfo summary(Long threadId, boolean pinned) {
        return new ThreadSummaryInfo(
            threadId, "스터디", null, CommunityThreadCategory.STUDY, "📚",
            3L, 2L, 100, pinned, false, true, CommunityThreadMemberRole.OWNER,
            new ThreadLastMessageInfo("최근 메시지", "하늘", NOW), 99L, NOW, NOW
        );
    }
}
