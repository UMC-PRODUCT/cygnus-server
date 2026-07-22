package com.umc.product.notice.adapter.in.web.dto;

import static com.umc.product.support.fixture.NoticeUnitFixture.challengerTarget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.adapter.in.web.dto.request.AddNoticeImagesRequest;
import com.umc.product.notice.adapter.in.web.dto.request.AddNoticeLinksRequest;
import com.umc.product.notice.adapter.in.web.dto.request.AddNoticeVoteRequest;
import com.umc.product.notice.adapter.in.web.dto.request.CreateNoticeRequest;
import com.umc.product.notice.adapter.in.web.dto.request.GetNoticeStatusRequest;
import com.umc.product.notice.adapter.in.web.dto.request.ReplaceNoticeImagesRequest;
import com.umc.product.notice.adapter.in.web.dto.request.ReplaceNoticeLinksRequest;
import com.umc.product.notice.adapter.in.web.dto.request.SendNoticeReminderRequest;
import com.umc.product.notice.adapter.in.web.dto.request.SubmitNoticeVoteResponseRequest;
import com.umc.product.notice.adapter.in.web.dto.request.UpdateNoticeRequest;
import com.umc.product.notice.adapter.in.web.dto.request.UpdateNoticeVoteResponseRequest;
import com.umc.product.notice.adapter.in.web.dto.response.command.AddNoticeImagesResponse;
import com.umc.product.notice.adapter.in.web.dto.response.command.AddNoticeLinksResponse;
import com.umc.product.notice.adapter.in.web.dto.response.command.AddNoticeVoteResponse;
import com.umc.product.notice.adapter.in.web.dto.response.command.CreateNoticeResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeDetailResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeReadStatusResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeStaticsResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeSummaryResponse;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeLinksCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteResult;
import com.umc.product.notice.application.port.in.command.dto.CreateNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.DeleteNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.RemoveNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.RemoveNoticeLinksCommand;
import com.umc.product.notice.application.port.in.command.dto.RemoveNoticeVotesCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeLinksCommand;
import com.umc.product.notice.application.port.in.command.dto.SendNoticeReminderCommand;
import com.umc.product.notice.application.port.in.command.dto.SubmitNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.query.dto.GetNoticeStatusQuery;
import com.umc.product.notice.application.port.in.query.dto.NoticeImageInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeLinkInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusResult;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeVoteInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeVoteInfo.VoteOptionInfo;
import com.umc.product.notice.domain.enums.NoticeReadStatus;
import com.umc.product.notice.domain.enums.NoticeReadStatusFilterType;
import com.umc.product.notice.domain.enums.VoteStatus;
import com.umc.product.notice.domain.exception.NoticeDomainException;

@DisplayName("Notice DTO 변환 테스트")
class NoticeDtoTest {

    @Test
    @DisplayName("모든 command DTO는 입력값을 손실 없이 보존한다")
    void command_DTO는_입력값을_보존한다() {
        Instant now = Instant.parse("2026-07-22T00:00:00Z");
        assertThat(new AddNoticeImagesCommand(List.of("image")).imageIds()).containsExactly("image");
        assertThat(new AddNoticeLinksCommand(List.of("link")).links()).containsExactly("link");
        assertThat(new RemoveNoticeImagesCommand(List.of(1L)).noticeImageIds()).containsExactly(1L);
        assertThat(new RemoveNoticeLinksCommand(List.of(2L)).noticeLinkIds()).containsExactly(2L);
        assertThat(new RemoveNoticeVotesCommand(List.of(3L)).noticeVoteIds()).containsExactly(3L);
        assertThat(new ReplaceNoticeImagesCommand(List.of("image")).imageIds()).containsExactly("image");
        assertThat(new ReplaceNoticeLinksCommand(List.of("link")).links()).containsExactly("link");
        assertThat(new DeleteNoticeCommand(1L, 2L).noticeId()).isEqualTo(2L);
        assertThat(new SendNoticeReminderCommand(1L, 2L, List.of(3L)).targetIds()).containsExactly(3L);
        assertThat(new AddNoticeVoteResult(1L, 2L).voteId()).isEqualTo(2L);
        assertThat(new CreateNoticeCommand(1L, "제목", "내용", true, true, challengerTarget()).mustRead()).isTrue();
        assertThat(new UpdateNoticeCommand(1L, 2L, "제목", "내용", true).noticeId()).isEqualTo(2L);
        assertThat(SubmitNoticeVoteResponseCommand.builder()
            .noticeId(1L).respondentMemberId(2L).selectedOptionIds(List.of(3L)).build().selectedOptionIds())
            .containsExactly(3L);
        assertThat(UpdateNoticeVoteResponseCommand.builder()
            .noticeId(1L).respondentMemberId(2L).selectedOptionIds(List.of()).build().selectedOptionIds()).isEmpty();
        assertThat(AddNoticeVoteCommand.builder()
            .createdMemberId(1L)
            .title("투표")
            .startsAt(now)
            .endsAtExclusive(now.plusSeconds(60))
            .options(List.of("A", "B"))
            .build().options()).containsExactly("A", "B");
    }

    @Test
    @DisplayName("투표 command는 선택지 개수와 빈 내용을 엄격히 검증한다")
    void 투표_command는_선택지를_검증한다() {
        assertThatThrownBy(() -> voteCommand(null)).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> voteCommand(List.of("A"))).isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> voteCommand(List.of("A", "B", "C", "D", "E", "F")))
            .isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> voteCommand(java.util.Arrays.asList("A", null)))
            .isInstanceOf(NoticeDomainException.class);
        assertThatThrownBy(() -> voteCommand(List.of("A", " "))).isInstanceOf(NoticeDomainException.class);
    }

    @Test
    @DisplayName("모든 웹 요청은 식별자와 본문을 application command/query로 변환한다")
    void 웹_요청을_command와_query로_변환한다() {
        Instant now = Instant.parse("2026-07-22T00:00:00Z");
        assertThat(new AddNoticeImagesRequest(List.of("image")).toCommand().imageIds()).containsExactly("image");
        assertThat(new AddNoticeLinksRequest(List.of("link")).toCommand().links()).containsExactly("link");
        assertThat(new ReplaceNoticeImagesRequest(List.of()).toCommand().imageIds()).isEmpty();
        assertThat(new ReplaceNoticeLinksRequest(List.of()).toCommand().links()).isEmpty();
        assertThat(new CreateNoticeRequest("제목", "내용", true, false, challengerTarget()).toCommand(1L).memberId())
            .isEqualTo(1L);
        assertThat(new UpdateNoticeRequest("제목", "내용", true).toCommand(1L, 2L).noticeId()).isEqualTo(2L);
        assertThat(new SendNoticeReminderRequest(List.of(3L)).toCommand(1L, 2L).targetIds()).containsExactly(3L);
        assertThat(new AddNoticeVoteRequest(
            "투표", false, true, now, now.plusSeconds(60), List.of("A", "B")
        ).toCommand(1L).createdMemberId()).isEqualTo(1L);
        assertThat(new SubmitNoticeVoteResponseRequest(List.of(3L)).toCommand(1L, 2L).selectedOptionIds())
            .containsExactly(3L);
        assertThat(new UpdateNoticeVoteResponseRequest(List.of()).toCommand(1L, 2L).selectedOptionIds()).isEmpty();
        GetNoticeStatusQuery statusQuery = new GetNoticeStatusRequest(
            1L, NoticeReadStatusFilterType.SCHOOL, List.of(2L), NoticeReadStatus.UNREAD
        ).toQuery(3L);
        assertThat(statusQuery).isEqualTo(new GetNoticeStatusQuery(
            1L, 3L, NoticeReadStatusFilterType.SCHOOL, List.of(2L), NoticeReadStatus.UNREAD
        ));
    }

    @Test
    @DisplayName("query DTO와 응답은 상세·요약·읽음·통계·투표 정보를 정확히 매핑한다")
    void query_DTO와_응답을_매핑한다() {
        Instant now = Instant.parse("2026-07-22T00:00:00Z");
        NoticeImageInfo image = new NoticeImageInfo(1L, "image", 0);
        NoticeLinkInfo link = new NoticeLinkInfo(2L, "link", 0);
        VoteOptionInfo option = new VoteOptionInfo(3L, "A", 4L, BigDecimal.TEN, List.of(5L));
        NoticeVoteInfo vote = new NoticeVoteInfo(
            6L, "투표", false, false, VoteStatus.OPEN, now, now.plusSeconds(60), 1L,
            List.of(option), List.of(3L)
        );
        NoticeInfo info = new NoticeInfo(
            7L, "제목", "내용", 8L, true, vote, List.of(image), List.of(link), challengerTarget(), 9L, now
        );
        GetNoticeDetailResponse detail = GetNoticeDetailResponse.from(info);
        assertThat(detail.id()).isEqualTo(7L);
        assertThat(detail.authorChallengerId()).isNull();
        assertThat(detail.authorMemberId()).isEqualTo(8L);
        assertThat(detail.viewCount()).isEqualTo(10L);
        assertThat(detail.vote().options()).containsExactly(option);
        assertThat(detail.images()).containsExactly(image);
        assertThat(detail.links()).containsExactly(link);

        NoticeSummary summary = new NoticeSummary(
            7L, "제목", "내용", true, true, 9L, now, challengerTarget(), 8L, "닉네임", "이름"
        );
        GetNoticeSummaryResponse summaryResponse = GetNoticeSummaryResponse.from(summary);
        assertThat(summaryResponse.authorChallengerId()).isNull();
        assertThat(summaryResponse.authorMemberId()).isEqualTo(8L);
        assertThat(summaryResponse.authorNickname()).isEqualTo("닉네임");

        NoticeReadStatusInfo readInfo = new NoticeReadStatusInfo(
            1L, "이름", "profile", ChallengerPart.SPRINGBOOT, 2L, "학교", 3L, "지부"
        );
        GetNoticeReadStatusResponse readResponse = GetNoticeReadStatusResponse.from(readInfo);
        assertThat(readResponse.chapterName()).isEqualTo("지부");
        assertThat(new NoticeReadStatusResult(List.of(readInfo), 1L, true).hasNext()).isTrue();

        NoticeReadStatusSummary statics = new NoticeReadStatusSummary(10, 4, 6, 40F);
        assertThat(GetNoticeStaticsResponse.from(statics).readRate()).isEqualTo(40F);
    }

    @Test
    @DisplayName("command 응답 DTO는 생성된 식별자를 그대로 노출한다")
    void command_응답은_ID를_노출한다() {
        assertThat(new CreateNoticeResponse(1L).noticeId()).isEqualTo(1L);
        assertThat(new AddNoticeImagesResponse(List.of(1L)).imageIds()).containsExactly(1L);
        assertThat(new AddNoticeLinksResponse(List.of(2L)).linkIds()).containsExactly(2L);
        assertThat(AddNoticeVoteResponse.from(new AddNoticeVoteResult(3L, 4L)))
            .isEqualTo(new AddNoticeVoteResponse(3L, 4L));
    }

    private static AddNoticeVoteCommand voteCommand(List<String> options) {
        return AddNoticeVoteCommand.builder()
            .createdMemberId(1L)
            .title("투표")
            .startsAt(Instant.parse("2026-07-22T00:00:00Z"))
            .endsAtExclusive(Instant.parse("2026-07-23T00:00:00Z"))
            .options(options)
            .build();
    }
}
