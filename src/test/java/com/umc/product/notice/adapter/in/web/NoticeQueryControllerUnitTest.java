package com.umc.product.notice.adapter.in.web;

import static com.umc.product.support.fixture.NoticeUnitFixture.challengerTarget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notice.adapter.in.web.assembler.NoticeViewerInfoAssembler;
import com.umc.product.notice.adapter.in.web.dto.request.GetNoticeStatusRequest;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.query.GetNoticeUseCase;
import com.umc.product.notice.application.port.in.query.dto.NoticeInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusResult;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.domain.NoticeClassification;
import com.umc.product.notice.domain.enums.NoticeReadStatus;
import com.umc.product.notice.domain.enums.NoticeReadStatusFilterType;
import com.umc.product.notice.domain.enums.NoticeTab;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeQueryController 단위 테스트")
class NoticeQueryControllerUnitTest {

    @Mock
    GetNoticeUseCase getNoticeUseCase;

    @Mock
    ManageNoticeUseCase manageNoticeUseCase;

    @Mock
    NoticeViewerInfoAssembler noticeViewerInfoAssembler;

    @Test
    @DisplayName("목록·검색은 조회자 정보를 조립하고 page 응답으로 변환한다")
    void maps_list_and_search_pages() {
        NoticeQueryController sut = sut();
        MemberPrincipal principal = new MemberPrincipal(10L);
        NoticeClassification classification = new NoticeClassification(
            1L, null, null, null, NoticeTab.CHALLENGER
        );
        NoticeViewerInfo viewer = new NoticeViewerInfo(Set.of(ChallengerPart.WEB), 2L, 3L, null);
        PageRequest pageable = PageRequest.of(0, 10);
        NoticeSummary summary = new NoticeSummary(
            1L, "제목", "내용", false, true, 5L, Instant.EPOCH, challengerTarget(), 10L, "닉네임", "이름"
        );
        given(noticeViewerInfoAssembler.toMemberIdAndGisuId(10L, 1L)).willReturn(viewer);
        given(getNoticeUseCase.getAllNoticeSummaries(viewer, classification, pageable))
            .willReturn(new PageImpl<>(List.of(summary), pageable, 1));
        given(getNoticeUseCase.searchNoticesByKeyword("검색", viewer, classification, pageable))
            .willReturn(new PageImpl<>(List.of(summary), pageable, 1));

        var list = sut.getAllNotices(classification, pageable, principal);
        var search = sut.searchNotices("검색", classification, pageable, principal);

        assertThat(list.content()).singleElement().extracting(item -> item.id()).isEqualTo(1L);
        assertThat(search.content()).singleElement().extracting(item -> item.authorNickname()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("상세 조회는 조회수를 증가시키고 통계·cursor 응답을 변환한다")
    void maps_detail_statistics_and_cursor() {
        NoticeQueryController sut = sut();
        MemberPrincipal principal = new MemberPrincipal(10L);
        NoticeInfo detail = new NoticeInfo(
            1L, "제목", "내용", 10L, true, null, List.of(), List.of(), challengerTarget(), 7L, Instant.EPOCH
        );
        given(getNoticeUseCase.getNoticeDetail(1L, 10L)).willReturn(detail);
        given(getNoticeUseCase.getReadStatistics(1L)).willReturn(new NoticeReadStatusSummary(10, 4, 6, 40F));
        NoticeReadStatusInfo status = new NoticeReadStatusInfo(
            2L, "회원", "image", ChallengerPart.WEB, 3L, "학교", 4L, "지부"
        );
        given(getNoticeUseCase.getReadStatus(any())).willReturn(new NoticeReadStatusResult(List.of(status), 2L, true));

        var response = sut.getNotice(1L, principal);
        var statistics = sut.getNoticeReadStatics(1L);
        var cursor = sut.getNoticeReadStatus(1L, new GetNoticeStatusRequest(
            null, NoticeReadStatusFilterType.ALL, null, NoticeReadStatus.UNREAD
        ));

        assertThat(response.viewCount()).isEqualTo(8L);
        assertThat(statistics.readRate()).isEqualTo(40F);
        assertThat(cursor.content()).singleElement().extracting(item -> item.chapterName()).isEqualTo("지부");
        assertThat(cursor.nextCursor()).isEqualTo(2L);
        assertThat(cursor.hasNext()).isTrue();
        verify(manageNoticeUseCase).incrementViewCount(1L);
        verify(getNoticeUseCase).getReadStatus(any());
    }

    private NoticeQueryController sut() {
        return new NoticeQueryController(getNoticeUseCase, manageNoticeUseCase, noticeViewerInfoAssembler);
    }
}
