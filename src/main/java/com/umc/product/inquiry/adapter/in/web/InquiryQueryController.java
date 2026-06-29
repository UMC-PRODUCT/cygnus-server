package com.umc.product.inquiry.adapter.in.web;

import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.inquiry.adapter.in.web.dto.response.InquiryListItemResponse;
import com.umc.product.inquiry.application.port.in.query.GetInquiryListUseCase;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.application.port.in.query.dto.InquirySummaryInfo;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inquiry | 문의사항 Query", description = "문의 단건/목록 조회, 메시지 내역 조회")
public class InquiryQueryController {

    private final GetInquiryListUseCase getInquiryListUseCase;

    @GetMapping
    public CursorResponse<InquiryListItemResponse> getList(
        @CurrentMember MemberPrincipal principal,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") @Min(1) int size,
        @RequestParam(required = false) InquiryStatus status,
        @RequestParam(required = false) InquiryTarget target,
        @RequestParam(required = false) InquiryCategory category
    ) {
        CursorResponse<InquirySummaryInfo> result = getInquiryListUseCase.getList(
            new GetInquiryListQuery(principal.getMemberId(), cursor, size, status, target, category));
        return CursorResponse.of(
            result.content().stream().map(InquiryListItemResponse::from).toList(),
            result.nextCursor(),
            result.hasNext()
        );
    }
}
