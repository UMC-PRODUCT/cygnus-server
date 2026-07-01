package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationFormResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationResultResponse;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/public")
@RequiredArgsConstructor
public class RecruitingPublicController {

    private final GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;
    private final GetRecruitingApplicationQueryUseCase getRecruitingApplicationQueryUseCase;

    @GetMapping("/forms")
    public List<RecruitingApplicationFormResponse> listPublicForms(
        @RequestParam Long gisuId,
        @RequestParam Long schoolId
    ) {
        return getRecruitingFormQueryUseCase.listPublicForms(gisuId, schoolId)
            .stream()
            .map(RecruitingApplicationFormResponse::from)
            .toList();
    }

    @GetMapping("/applications/result")
    public RecruitingApplicationResultResponse getAnonymousResult(
        @ParameterObject AnonymousResultRequest request
    ) {
        return RecruitingApplicationResultResponse.from(
            getRecruitingApplicationQueryUseCase.getAnonymousResult(
                request.applicationNo(),
                request.applicantIdentityKey()
            )
        );
    }

    public record AnonymousResultRequest(
        String applicationNo,
        String applicantIdentityKey
    ) {
    }
}
