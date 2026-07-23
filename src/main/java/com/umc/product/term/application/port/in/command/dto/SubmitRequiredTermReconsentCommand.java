package com.umc.product.term.application.port.in.command.dto;

public record SubmitRequiredTermReconsentCommand(Long memberId, Long termId) {

    public static SubmitRequiredTermReconsentCommand of(Long memberId, Long termId) {
        return new SubmitRequiredTermReconsentCommand(memberId, termId);
    }
}
