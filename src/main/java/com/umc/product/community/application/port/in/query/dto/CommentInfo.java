package com.umc.product.community.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.domain.Comment;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.Builder;

@Builder
public record CommentInfo(
    Long commentId,
    Long postId,
    Long challengerId,
    String challengerName,
    String challengerNickname,
    String challengerProfileImage,
    ChallengerPart challengerPart,
    String content,
    Instant createdAt,
    boolean isAuthor
) {
    public static CommentInfo of(Comment comment, MemberInfo memberInfo, ChallengerInfo challengerInfo) {
        return of(comment, memberInfo, challengerInfo, false);
    }

    public static CommentInfo of(
        Comment comment,
        MemberInfo memberInfo,
        ChallengerInfo challengerInfo,
        boolean isAuthor
    ) {
        String name = memberInfo != null ? memberInfo.name() : null;
        String nickname = memberInfo != null ? memberInfo.nickname() : null;
        String profileImage = memberInfo != null ? memberInfo.profileImageLink() : null;
        Long challengerId = challengerInfo != null ? challengerInfo.challengerId() : null;
        ChallengerPart part = challengerInfo != null ? challengerInfo.part() : null;

        return CommentInfo.builder()
            .commentId(comment.getId())
            .postId(comment.getPostId())
            .challengerId(challengerId)
            .challengerName(name)
            .challengerNickname(nickname)
            .challengerProfileImage(profileImage)
            .challengerPart(part)
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .isAuthor(isAuthor)
            .build();
    }

    public static CommentInfo from(Comment comment, String challengerName) {
        return from(comment, challengerName, null, null, false);
    }

    public static CommentInfo from(Comment comment, String challengerName, String challengerProfileImage) {
        return from(comment, challengerName, challengerProfileImage, null, false);
    }

    public static CommentInfo from(
        Comment comment, String challengerName, String challengerProfileImage,
        ChallengerPart challengerPart
    ) {
        return from(comment, challengerName, challengerProfileImage, challengerPart, false);
    }

    public static CommentInfo from(
        Comment comment, String challengerName, String challengerProfileImage,
        ChallengerPart challengerPart, boolean isAuthor
    ) {
        Long id = comment.getId();

        return CommentInfo.builder()
            .commentId(id)
            .postId(comment.getPostId())
            .challengerId(comment.getChallengerId())
            .challengerName(challengerName)
            .challengerProfileImage(challengerProfileImage)
            .challengerPart(challengerPart)
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .isAuthor(isAuthor)
            .build();
    }
}
