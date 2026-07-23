package com.umc.product.term.application.port.in.command;

import com.umc.product.term.application.port.in.command.dto.SubmitRequiredTermReconsentCommand;

public interface SubmitRequiredTermReconsentUseCase {

    /**
     * 회원이 현재 활성화된 필수 약관에 재동의합니다.
     */
    void submitRequiredTermReconsent(SubmitRequiredTermReconsentCommand command);
}
