package com.umc.product.notice.adapter.in.web;

import static com.umc.product.support.fixture.NoticeUnitFixture.challengerTarget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notice.adapter.in.web.dto.request.CreateNoticeRequest;
import com.umc.product.notice.adapter.in.web.dto.request.SendNoticeReminderRequest;
import com.umc.product.notice.adapter.in.web.dto.request.UpdateNoticeRequest;
import com.umc.product.notice.application.port.in.command.ManageNoticeReadUseCase;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.command.dto.DeleteNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.SendNoticeReminderCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeCommand;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeCommandController 단위 테스트")
class NoticeCommandControllerUnitTest {

    @Mock
    ManageNoticeUseCase manageNoticeUseCase;

    @Mock
    ManageNoticeReadUseCase manageNoticeReadUseCase;

    @Test
    @DisplayName("현재 회원 ID와 요청을 모든 공지 command로 정확히 변환한다")
    void delegates_all_commands_with_current_member() {
        NoticeCommandController sut = new NoticeCommandController(manageNoticeUseCase, manageNoticeReadUseCase);
        MemberPrincipal principal = new MemberPrincipal(10L);
        given(manageNoticeUseCase.createNotice(any())).willReturn(100L);

        var created = sut.createNotice(
            new CreateNoticeRequest("제목", "내용", true, false, challengerTarget()), principal
        );
        sut.deleteNotice(100L, principal);
        sut.updateNotice(100L, new UpdateNoticeRequest("수정", "수정 내용", true), principal);
        sut.sendNoticeReminder(100L, new SendNoticeReminderRequest(List.of(1L, 2L)), principal);
        sut.recordNoticeRead(100L, principal);

        assertThat(created.noticeId()).isEqualTo(100L);
        verify(manageNoticeUseCase).deleteNotice(new DeleteNoticeCommand(10L, 100L));
        verify(manageNoticeUseCase).updateNoticeTitleOrContent(new UpdateNoticeCommand(10L, 100L, "수정", "수정 내용", true));
        verify(manageNoticeUseCase).remindNotice(new SendNoticeReminderCommand(10L, 100L, List.of(1L, 2L)));
        verify(manageNoticeReadUseCase).recordRead(100L, 10L);
    }
}
