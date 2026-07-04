package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.command.dto.CreateVoteCommand;

public interface ManageVoteUseCase {

    /**
     * 투표용 설문을 생성합니다. (1섹션 1질문 구조)
     */
    Long createVote(CreateVoteCommand command);

    /**
     * 특정 투표(설문)을 삭제합니다.
     */
    void deleteVote(Long formId);
}
