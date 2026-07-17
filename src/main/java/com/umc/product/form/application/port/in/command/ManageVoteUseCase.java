package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.dto.CreateVoteCommand;
import com.umc.product.form.domain.FormOwnerReference;

public interface ManageVoteUseCase {

    /**
     * 투표용 설문을 생성합니다. (1섹션 1질문 구조)
     */
    Long createVote(
        FormOwnerReferenceFactory ownerFactory,
        FormActorContext actorContext,
        CreateVoteCommand command
    );

    /**
     * 특정 투표(설문)을 삭제합니다.
     */
    void deleteVote(FormOwnerReference expectedOwner, FormActorContext actorContext);
}
