package com.umc.product.notice.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageVoteUseCase;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.notice.application.policy.NoticeVoteOwnerReferenceFactory;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteCommand;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.LoadNoticeLinkPort;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.application.port.out.SaveNoticeImagePort;
import com.umc.product.notice.application.port.out.SaveNoticeLinkPort;
import com.umc.product.notice.application.port.out.SaveNoticeVotePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeVote;

@DisplayName("NoticeContentService Form ownership bridge")
class NoticeContentServiceTest {

    private static final Long NOTICE_ID = 42L;
    private static final Long AUTHOR_ID = 7L;

    private LoadNoticeVotePort loadNoticeVotePort;
    private SaveNoticeVotePort saveNoticeVotePort;
    private LoadNoticePort loadNoticePort;
    private ManageVoteUseCase manageVoteUseCase;
    private NoticeContentService sut;

    @BeforeEach
    void setUp() {
        loadNoticeVotePort = mock(LoadNoticeVotePort.class);
        LoadNoticeLinkPort loadNoticeLinkPort = mock(LoadNoticeLinkPort.class);
        LoadNoticeImagePort loadNoticeImagePort = mock(LoadNoticeImagePort.class);
        saveNoticeVotePort = mock(SaveNoticeVotePort.class);
        SaveNoticeImagePort saveNoticeImagePort = mock(SaveNoticeImagePort.class);
        SaveNoticeLinkPort saveNoticeLinkPort = mock(SaveNoticeLinkPort.class);
        loadNoticePort = mock(LoadNoticePort.class);
        manageVoteUseCase = mock(ManageVoteUseCase.class);

        sut = new NoticeContentService(
            loadNoticeVotePort,
            loadNoticeLinkPort,
            loadNoticeImagePort,
            saveNoticeVotePort,
            saveNoticeImagePort,
            saveNoticeLinkPort,
            loadNoticePort,
            manageVoteUseCase
        );
    }

    @Test
    @DisplayName("Form 생성에는 trusted owner factory와 server actor만 전달하고 "
        + "NoticeVote scalar를 같은 흐름에서 저장한다")
    void createsFormAndNoticeVoteWithTrustedBinding() {
        Notice notice = notice();
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice));
        given(loadNoticeVotePort.existsVoteByNoticeId(NOTICE_ID)).willReturn(false);
        given(manageVoteUseCase.createVote(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).willReturn(900L);
        NoticeVote savedVote = NoticeVote.create(900L, notice, Instant.now(), Instant.now().plusSeconds(60));
        given(saveNoticeVotePort.saveVote(org.mockito.ArgumentMatchers.any())).willReturn(savedVote);

        sut.addVote(command(), NOTICE_ID);

        ArgumentCaptor<FormOwnerReferenceFactory> factoryCaptor = ArgumentCaptor.forClass(
            FormOwnerReferenceFactory.class);
        ArgumentCaptor<FormActorContext> actorCaptor = ArgumentCaptor.forClass(FormActorContext.class);
        verify(manageVoteUseCase).createVote(
            factoryCaptor.capture(),
            actorCaptor.capture(),
            org.mockito.ArgumentMatchers.any()
        );

        FormOwnerReference reference = factoryCaptor.getValue().create(900L);
        assertThat(reference).isEqualTo(NoticeVoteOwnerReferenceFactory.expectedOwner(NOTICE_ID, 900L));
        assertThat(actorCaptor.getValue()).isEqualTo(FormActorContext.authenticated(AUTHOR_ID));
        verify(saveNoticeVotePort).saveVote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Form 또는 ownership binding 생성이 실패하면 consumer NoticeVote를 저장하지 않는다")
    void rollsBackNoticeScalarWhenFormCreationFails() {
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice()));
        given(loadNoticeVotePort.existsVoteByNoticeId(NOTICE_ID)).willReturn(false);
        given(manageVoteUseCase.createVote(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).willThrow(new IllegalStateException("ownership binding failed"));

        assertThatThrownBy(() -> sut.addVote(command(), NOTICE_ID))
            .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(saveNoticeVotePort);
    }

    private Notice notice() {
        Notice notice = Notice.create("title", "content", AUTHOR_ID, false, false);
        ReflectionTestUtils.setField(notice, "id", NOTICE_ID);
        return notice;
    }

    private AddNoticeVoteCommand command() {
        return AddNoticeVoteCommand.builder()
            .createdMemberId(AUTHOR_ID)
            .title("vote")
            .isAnonymous(false)
            .allowMultipleChoice(false)
            .startsAt(Instant.now())
            .endsAtExclusive(Instant.now().plusSeconds(60))
            .options(java.util.List.of("yes", "no"))
            .build();
    }
}
