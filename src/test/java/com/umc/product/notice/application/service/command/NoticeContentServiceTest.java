package com.umc.product.notice.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
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
import com.umc.product.notice.application.port.in.command.dto.AddNoticeImagesCommand;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteCommand;
import com.umc.product.notice.application.port.in.command.dto.ReplaceNoticeImagesCommand;
import com.umc.product.notice.application.port.out.LoadNoticeImagePort;
import com.umc.product.notice.application.port.out.LoadNoticeLinkPort;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.application.port.out.SaveNoticeImagePort;
import com.umc.product.notice.application.port.out.SaveNoticeLinkPort;
import com.umc.product.notice.application.port.out.SaveNoticeVotePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeImage;
import com.umc.product.notice.domain.NoticeVote;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@DisplayName("NoticeContentService command")
class NoticeContentServiceTest {

    private static final Long NOTICE_ID = 42L;
    private static final Long AUTHOR_ID = 7L;

    private LoadNoticeVotePort loadNoticeVotePort;
    private LoadNoticeImagePort loadNoticeImagePort;
    private SaveNoticeVotePort saveNoticeVotePort;
    private SaveNoticeImagePort saveNoticeImagePort;
    private LoadNoticePort loadNoticePort;
    private ManageVoteUseCase manageVoteUseCase;
    private ManageFileUsageUseCase manageFileUsageUseCase;
    private NoticeContentService sut;

    @BeforeEach
    void setUp() {
        loadNoticeVotePort = mock(LoadNoticeVotePort.class);
        LoadNoticeLinkPort loadNoticeLinkPort = mock(LoadNoticeLinkPort.class);
        loadNoticeImagePort = mock(LoadNoticeImagePort.class);
        saveNoticeVotePort = mock(SaveNoticeVotePort.class);
        saveNoticeImagePort = mock(SaveNoticeImagePort.class);
        SaveNoticeLinkPort saveNoticeLinkPort = mock(SaveNoticeLinkPort.class);
        loadNoticePort = mock(LoadNoticePort.class);
        manageVoteUseCase = mock(ManageVoteUseCase.class);
        manageFileUsageUseCase = mock(ManageFileUsageUseCase.class);

        sut = new NoticeContentService(
            loadNoticeVotePort,
            loadNoticeLinkPort,
            loadNoticeImagePort,
            saveNoticeVotePort,
            saveNoticeImagePort,
            saveNoticeLinkPort,
            loadNoticePort,
            manageVoteUseCase,
            manageFileUsageUseCase
        );
    }

    @Test
    @DisplayName("공지 이미지를 추가하면 mutation 후 기존 이미지까지 포함한 "
        + "전체 set을 동기화한다")
    void addsNoticeImagesAndSynchronizesWholeStoredSnapshot() {
        Notice notice = notice();
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice));
        given(loadNoticeImagePort.countImageByNoticeId(NOTICE_ID)).willReturn(1);
        given(loadNoticeImagePort.findNextImageDisplayOrder(NOTICE_ID)).willReturn(1);
        given(saveNoticeImagePort.saveAllImages(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(loadNoticeImagePort.findImagesByNoticeId(NOTICE_ID)).willReturn(List.of(
            NoticeImage.create("file-a", notice, 0),
            NoticeImage.create("file-b", notice, 1),
            NoticeImage.create("file-c", notice, 2)
        ));

        sut.addImages(new AddNoticeImagesCommand(List.of("file-b", "file-c")), NOTICE_ID, AUTHOR_ID);

        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(usageCaptor.capture());
        assertThat(usageCaptor.getValue().fileIds())
            .containsExactlyInAnyOrder("file-a", "file-b", "file-c");
        assertThat(usageCaptor.getValue().requesterMemberId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    @DisplayName("공지 이미지 [A,B]를 [B,C]로 교체하면 저장된 전체 set만 usage snapshot이 된다")
    void replacesNoticeImageUsageWithExactStoredSnapshot() {
        Notice notice = notice();
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice));
        given(loadNoticeImagePort.findImagesByNoticeId(NOTICE_ID)).willReturn(List.of(
            NoticeImage.create("file-b", notice, 0),
            NoticeImage.create("file-c", notice, 1)
        ));

        sut.replaceImages(
            new ReplaceNoticeImagesCommand(List.of("file-b", "file-c")), NOTICE_ID, AUTHOR_ID);

        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(usageCaptor.capture());
        assertThat(usageCaptor.getValue().owner())
            .isEqualTo(FileUsageCoordinate.of("notice", NOTICE_ID.toString(), "images"));
        assertThat(usageCaptor.getValue().fileIds()).containsExactlyInAnyOrder("file-b", "file-c");
        assertThat(usageCaptor.getValue().requesterMemberId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    @DisplayName("공지 이미지를 빈 목록으로 교체하면 image usage snapshot을 비운다")
    void clearsNoticeImageUsageWhenImagesAreDeleted() {
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice()));
        given(loadNoticeImagePort.findImagesByNoticeId(NOTICE_ID)).willReturn(List.of());

        sut.replaceImages(new ReplaceNoticeImagesCommand(List.of()), NOTICE_ID, AUTHOR_ID);

        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(usageCaptor.capture());
        assertThat(usageCaptor.getValue().fileIds()).isEmpty();
        then(saveNoticeImagePort).should(never()).saveAllImages(any());
    }

    @Test
    @DisplayName("공지 삭제용 content 일괄 제거도 image usage snapshot을 비운다")
    void detachesNoticeImageUsageWhenAllContentsAreRemoved() {
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice()));
        given(loadNoticeVotePort.findVoteByNoticeId(NOTICE_ID)).willReturn(Optional.empty());
        given(loadNoticeImagePort.findImagesByNoticeId(NOTICE_ID)).willReturn(List.of());

        sut.removeContentsByNoticeId(NOTICE_ID, AUTHOR_ID);

        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(usageCaptor.capture());
        assertThat(usageCaptor.getValue().owner())
            .isEqualTo(FileUsageCoordinate.of("notice", NOTICE_ID.toString(), "images"));
        assertThat(usageCaptor.getValue().fileIds()).isEmpty();
    }

    @Test
    @DisplayName("usage가 requester의 파일 사용을 거부하면 이미지 mutation 흐름도 실패한다")
    void propagatesWrongUploaderFailureAfterImageMutation() {
        Notice notice = notice();
        given(loadNoticePort.findNoticeById(NOTICE_ID)).willReturn(Optional.of(notice));
        given(loadNoticeImagePort.findImagesByNoticeId(NOTICE_ID))
            .willReturn(List.of(NoticeImage.create("other-file", notice, 0)));
        willThrow(new StorageException(StorageErrorCode.FILE_USE_FORBIDDEN))
            .given(manageFileUsageUseCase).replaceUsages(any());

        assertThatThrownBy(() -> sut.replaceImages(
            new ReplaceNoticeImagesCommand(List.of("other-file")), NOTICE_ID, AUTHOR_ID))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_USE_FORBIDDEN);

        then(saveNoticeImagePort).should().saveAllImages(any());
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
