package com.umc.product.storage.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.storage.adapter.in.graphql.dto.FileUploadGraphQlResponse;
import com.umc.product.storage.adapter.in.graphql.dto.PrepareFileUploadGraphQlRequest;
import com.umc.product.storage.application.port.in.command.ManageFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StorageGraphQlController {

    private final ManageFileUseCase manageFileUseCase;

    @MutationMapping
    public FileUploadGraphQlResponse prepareFileUpload(
        @CurrentMember MemberPrincipal principal,
        @Argument PrepareFileUploadGraphQlRequest input
    ) {
        return FileUploadGraphQlResponse.from(
            manageFileUseCase.getFileUploadUrl(input.toCommand(principal.getMemberId()))
        );
    }

    @MutationMapping
    public FileOperationPayload confirmFileUpload(
        @CurrentMember MemberPrincipal principal,
        @Argument String fileId
    ) {
        manageFileUseCase.confirmUpload(fileId);
        return new FileOperationPayload(fileId);
    }

    @MutationMapping
    public FileOperationPayload deleteFile(
        @CurrentMember MemberPrincipal principal,
        @Argument String fileId
    ) {
        manageFileUseCase.deleteFile(DeleteFileCommand.builder()
            .fileId(fileId)
            .requesterMemberId(principal.getMemberId())
            .build());
        return new FileOperationPayload(fileId);
    }

    public record FileOperationPayload(String fileId) {
    }
}
