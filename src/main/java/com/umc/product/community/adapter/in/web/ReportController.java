package com.umc.product.community.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.community.application.port.in.command.report.ReportCommentUseCase;
import com.umc.product.community.application.port.in.command.report.ReportPostUseCase;
import com.umc.product.community.application.port.in.command.report.dto.ReportCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportPostCommand;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Community | 신고", description = "게시글과 댓글 신고를 접수합니다.")
public class ReportController {

    private final ReportPostUseCase reportPostUseCase;
    private final ReportCommentUseCase reportCommentUseCase;

    @PostMapping("/posts/{postId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "REPORT-001", summary = "게시글 신고", description = "게시글을 신고합니다. 같은 게시글은 한 번만 신고할 수 있습니다.")
    public void reportPost(
        @PathVariable Long postId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        ReportPostCommand command = new ReportPostCommand(postId, memberPrincipal.getMemberId());
        reportPostUseCase.report(command);
    }

    @PostMapping("/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "REPORT-002", summary = "댓글 신고", description = "댓글을 신고합니다. 같은 댓글은 한 번만 신고할 수 있습니다.")
    public void reportComment(
        @PathVariable Long commentId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        ReportCommentCommand command = new ReportCommentCommand(commentId, memberPrincipal.getMemberId());
        reportCommentUseCase.report(command);
    }
}
