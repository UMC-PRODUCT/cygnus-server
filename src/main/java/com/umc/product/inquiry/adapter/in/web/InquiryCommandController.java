package com.umc.product.inquiry.adapter.in.web;

import com.umc.product.inquiry.adapter.in.web.dto.request.AssignInquiryManagerRequest;
import com.umc.product.inquiry.adapter.in.web.dto.request.SubmitInquiryRequest;
import com.umc.product.inquiry.adapter.in.web.dto.request.TransferInquiryManagerRequest;
import com.umc.product.inquiry.adapter.in.web.dto.response.InquiryResponse;
import com.umc.product.inquiry.application.port.in.command.AssignInquiryManagerUseCase;
import com.umc.product.inquiry.application.port.in.command.CloseInquiryUseCase;
import com.umc.product.inquiry.application.port.in.command.SubmitInquiryUseCase;
import com.umc.product.inquiry.application.port.in.command.TransferInquiryManagerUseCase;
import com.umc.product.inquiry.application.port.in.command.dto.CloseInquiryCommand;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry | 문의사항 Command", description = "문의 등록, 종료")
public class InquiryCommandController {

    private final SubmitInquiryUseCase submitInquiryUseCase;
    private final AssignInquiryManagerUseCase assignInquiryManagerUseCase;
    private final TransferInquiryManagerUseCase transferInquiryManagerUseCase;
    private final CloseInquiryUseCase closeInquiryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse submit(
        @Valid @RequestBody SubmitInquiryRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        return InquiryResponse.from(
            submitInquiryUseCase.submit(request.toCommand(principal.getMemberId())));
    }

    @PostMapping("/{inquiryId}/managers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignManager(
        @PathVariable Long inquiryId,
        @Valid @RequestBody AssignInquiryManagerRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        assignInquiryManagerUseCase.assign(request.toCommand(inquiryId, principal.getMemberId()));
    }

    @PatchMapping("/{inquiryId}/managers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferManager(
        @PathVariable Long inquiryId,
        @Valid @RequestBody TransferInquiryManagerRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        transferInquiryManagerUseCase.transfer(request.toCommand(inquiryId, principal.getMemberId()));
    }

    @PatchMapping("/{inquiryId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(
        @PathVariable Long inquiryId,
        @CurrentMember MemberPrincipal principal
    ) {
        closeInquiryUseCase.close(CloseInquiryCommand.of(inquiryId, principal.getMemberId()));
    }
}
