package com.umc.product.community.application.service.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.community.application.port.in.command.report.ReportCommentUseCase;
import com.umc.product.community.application.port.in.command.report.ReportPostUseCase;
import com.umc.product.community.application.port.in.command.report.dto.ReportCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportPostCommand;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.report.LoadReportPort;
import com.umc.product.community.application.port.out.report.SaveReportPort;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandService implements ReportPostUseCase, ReportCommentUseCase {

    private final LoadPostPort loadPostPort;
    private final LoadCommentPort loadCommentPort;
    private final LoadReportPort loadReportPort;
    private final SaveReportPort saveReportPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final RecordAuditLogUseCase recordAuditLogUseCase;

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.SUBMIT,
        targetType = "PostReport",
        targetId = "#command.postId()",
        description = "'커뮤니티 게시글 신고를 제출했습니다.'"
    )
    @Override
    public void report(ReportPostCommand command) {
        // 게시글 존재 확인
        Post post = loadPostPort.findById(command.postId())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.POST_NOT_FOUND));

        // 중복 신고 확인 및 저장
        checkDuplicateAndSaveReport(command.reporterId(), ReportTargetType.POST, command.postId());
        recordReportAudit(
            command.reporterId(),
            ReportTargetType.POST,
            command.postId(),
            post.getAuthorChallengerId()
        );
    }

    @Audited(
        domain = Domain.COMMUNITY,
        action = AuditAction.SUBMIT,
        targetType = "CommentReport",
        targetId = "#command.commentId()",
        description = "'커뮤니티 댓글 신고를 제출했습니다.'"
    )
    @Override
    public void report(ReportCommentCommand command) {
        // 댓글 존재 확인
        Comment comment = loadCommentPort.findById(command.commentId())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND));

        // 중복 신고 확인 및 저장
        checkDuplicateAndSaveReport(command.reporterId(), ReportTargetType.COMMENT, command.commentId());
        recordReportAudit(
            command.reporterId(),
            ReportTargetType.COMMENT,
            command.commentId(),
            comment.getChallengerId()
        );
    }

    /**
     * 중복 신고를 확인하고 신고를 생성합니다.
     *
     * @param reporterId 신고자 회원 ID
     * @param targetType 신고 대상 타입
     * @param targetId   신고 대상 ID
     * @throws BusinessException 이미 신고한 경우
     */
    private void checkDuplicateAndSaveReport(Long reporterId, ReportTargetType targetType, Long targetId) {
        // 중복 신고 확인
        if (loadReportPort.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
            throw new CommunityDomainException(CommunityErrorCode.REPORT_ALREADY_EXISTS);
        }

        // 신고 생성 및 저장
        Report report = Report.create(reporterId, targetType, targetId, null);
        saveReportPort.save(report);
    }

    private void recordReportAudit(
        Long reporterChallengerId,
        ReportTargetType targetType,
        Long targetId,
        Long authorChallengerId
    ) {
        MemberInfo reporter = findMemberByChallengerId(reporterChallengerId).orElse(null);
        MemberInfo author = findMemberByChallengerId(authorChallengerId).orElse(null);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("resourceType", "Challenger");
        context.put("resourceId", authorChallengerId);

        Map<String, Object> details = RecordAuditLogCommand.structuredDetails(
            memberSnapshot(reporter),
            targetSnapshot(targetType, targetId, author),
            context,
            Map.of(),
            Map.of()
        );
        recordAuditLogUseCase.record(RecordAuditLogCommand.success(
            Domain.COMMUNITY,
            AuditAction.SUBMIT,
            targetType == ReportTargetType.POST ? "PostReport" : "CommentReport",
            targetId.toString(),
            reporter == null ? null : reporter.id(),
            targetType == ReportTargetType.POST
                ? "커뮤니티 게시글 신고를 제출했습니다."
                : "커뮤니티 댓글 신고를 제출했습니다.",
            details
        ));
    }

    private Optional<MemberInfo> findMemberByChallengerId(Long challengerId) {
        if (challengerId == null) {
            return Optional.empty();
        }
        return getChallengerUseCase.findById(challengerId)
            .map(ChallengerInfo::memberId)
            .flatMap(getMemberUseCase::findById);
    }

    private Map<String, Object> memberSnapshot(MemberInfo memberInfo) {
        if (memberInfo == null) {
            return Map.of();
        }
        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("type", "Member");
        actor.put("memberId", memberInfo.id());
        actor.put("name", memberInfo.name());
        actor.put("nickname", memberInfo.nickname());
        actor.put("schoolName", memberInfo.schoolName());
        return actor;
    }

    private Map<String, Object> targetSnapshot(
        ReportTargetType targetType,
        Long targetId,
        MemberInfo author
    ) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", targetType.name());
        target.put("id", targetId);
        if (author != null) {
            target.put("memberId", author.id());
            target.put("name", author.name());
            target.put("nickname", author.nickname());
            target.put("schoolName", author.schoolName());
        }
        return target;
    }
}
