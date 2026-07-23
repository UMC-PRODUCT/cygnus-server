package com.umc.product.notice.adapter.in.graphql.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.application.port.in.query.dto.NoticeImageInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeLinkInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeSummary;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.enums.VoteStatus;

public record NoticeGraphQlResponse(
    Long id,
    String title,
    String content,
    Long authorMemberId,
    boolean mustRead,
    Target target,
    long viewCount,
    Instant createdAt,
    Vote vote,
    List<NoticeImageInfo> images,
    List<NoticeLinkInfo> links
) {

    public static NoticeGraphQlResponse from(NoticeSummary info) {
        return new NoticeGraphQlResponse(
            info.id(),
            info.title(),
            info.content(),
            info.authorMemberId(),
            info.mustRead(),
            Target.from(info.targetInfo()),
            info.viewCount(),
            info.createdAt(),
            null,
            List.of(),
            List.of()
        );
    }

    public static NoticeGraphQlResponse from(NoticeInfo info) {
        return new NoticeGraphQlResponse(
            info.id(),
            info.title(),
            info.content(),
            info.authorMemberId(),
            info.mustRead(),
            Target.from(info.targetInfo()),
            info.viewCount(),
            info.createdAt(),
            Vote.from(info.vote()),
            info.images(),
            info.links()
        );
    }

    public record Target(
        Long gisuId,
        Long chapterId,
        Long schoolId,
        List<ChallengerPart> parts,
        NoticeTab tab
    ) {

        private static Target from(NoticeTargetInfo info) {
            return new Target(
                info.targetGisuId(),
                info.targetChapterId(),
                info.targetSchoolId(),
                info.targetParts() == null ? List.of() : info.targetParts(),
                info.targetNoticeTab()
            );
        }
    }

    public record Vote(
        Long voteId,
        String title,
        boolean anonymous,
        boolean allowMultipleChoice,
        VoteStatus status,
        Instant startsAt,
        Instant endsAtExclusive,
        long totalParticipants,
        List<Option> options,
        List<Long> mySelectedOptionIds
    ) {

        private static Vote from(
            com.umc.product.notice.application.port.in.query.dto.NoticeVoteInfo info
        ) {
            return info == null
                ? null
                : new Vote(
                    info.voteId(),
                    info.title(),
                    info.isAnonymous(),
                    info.allowMultipleChoice(),
                    info.status(),
                    info.startsAt(),
                    info.endsAtExclusive(),
                    info.totalParticipants(),
                    info.options().stream().map(Option::from).toList(),
                    info.mySelectedOptionIds()
                );
        }
    }

    public record Option(
        Long optionId,
        String content,
        long voteCount,
        BigDecimal voteRate
    ) {

        private static Option from(
            com.umc.product.notice.application.port.in.query.dto.NoticeVoteInfo.VoteOptionInfo info
        ) {
            return new Option(
                info.optionId(),
                info.content(),
                info.voteCount(),
                info.voteRate()
            );
        }
    }
}
