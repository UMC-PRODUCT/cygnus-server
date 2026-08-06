package com.umc.product.notice.application.port.in.command;

import com.umc.product.notice.application.port.in.command.dto.SubmitNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeVoteResponseCommand;

public interface ManageNoticeVoteResponseUseCase {

    /**
     * 공지사항 투표에 응답을 제출합니다.
     * 투표 기간(OPEN) 및 중복 응답 여부를 검증한 후 Form UseCase로 위임합니다.
     *
     * @return 생성된 FormResponse ID
     */
    Long submit(SubmitNoticeVoteResponseCommand command);

    /**
     * 공지사항 투표 응답을 수정하거나 취소합니다.
     * {@code selectedOptionIds}가 비어있으면 기존 응답 취소, 그렇지 않으면 수정.
     */
    void updateOrCancel(UpdateNoticeVoteResponseCommand command);
}
