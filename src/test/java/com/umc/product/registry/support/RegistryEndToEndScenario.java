package com.umc.product.registry.support;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.chat.application.port.in.command.DeleteChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomOwnershipPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageAnswerUseCase;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.ManageFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.in.command.dto.PrepareFileUploadCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.enums.FileCategory;

public class RegistryEndToEndScenario {

    public static final long MEMBER_ID = 10L;
    public static final String CHAT_NAMESPACE = "e2e.chat-room";

    @Autowired
    private ManageFileUseCase manageFileUseCase;
    @Autowired
    private ManageFileUsageUseCase manageFileUsageUseCase;
    @Autowired
    private ManageFormUseCase manageFormUseCase;
    @Autowired
    private ManageFormSectionUseCase manageFormSectionUseCase;
    @Autowired
    private ManageQuestionUseCase manageQuestionUseCase;
    @Autowired
    private ManageFormResponseUseCase manageFormResponseUseCase;
    @Autowired
    private ManageAnswerUseCase manageAnswerUseCase;
    @Autowired
    private SaveChatRoomPort saveChatRoomPort;
    @Autowired
    private SaveChatRoomOwnershipPort saveChatRoomOwnershipPort;
    @Autowired
    private SaveChatMemberPort saveChatMemberPort;
    @Autowired
    private SendChatMessageUseCase sendChatMessageUseCase;
    @Autowired
    private DeleteChatRoomUseCase deleteChatRoomUseCase;

    public UploadedFile preparePendingPdf() {
        FileUploadInfo upload = manageFileUseCase.getFileUploadUrl(new PrepareFileUploadCommand(
            "registry-e2e.pdf",
            "application/pdf",
            1024L,
            FileCategory.POST_ATTACHMENT,
            MEMBER_ID
        ));
        String storageKey = FileCategory.POST_ATTACHMENT.getPathPrefix()
            + "/" + upload.fileId() + ".pdf";
        return new UploadedFile(upload.fileId(), storageKey);
    }

    public void confirmPdf(UploadedFile file, ControllableStoragePort storage) {
        storage.completeUpload(file.storageKey(), 1024L, "application/pdf");
        manageFileUseCase.confirmUpload(file.fileId());
    }

    public UploadedFile uploadConfirmedPdf(ControllableStoragePort storage) {
        UploadedFile file = preparePendingPdf();
        confirmPdf(file, storage);
        return file;
    }

    public FormFixture createForm() {
        FormOwnerReferenceFactory factory = FormOwnerReferenceFactory.standalone();
        Long formId = manageFormUseCase.createDraft(
            factory,
            formActor(),
            CreateDraftFormCommand.builder()
                .title("registry E2E attachment")
                .allowDuplicateResponses(true)
                .build()
        );
        return new FormFixture(formId, factory.create(formId));
    }

    public void addSectionWithWrongNamespace(FormFixture form) {
        manageFormSectionUseCase.createSection(
            form.owner().withNamespace("project.application-form"),
            formActor(),
            sectionCommand(form.formId())
        );
    }

    public FormAttachment attachForm(FormFixture form, String fileId) {
        Long sectionId = manageFormSectionUseCase.createSection(
            form.owner(), formActor(), sectionCommand(form.formId()));
        Long questionId = manageQuestionUseCase.createQuestion(
            form.owner(),
            formActor(),
            CreateQuestionCommand.builder()
                .sectionId(sectionId)
                .type(QuestionType.FILE)
                .title("attachment")
                .isRequired(false)
                .build()
        );
        manageFormUseCase.publishForm(
            form.owner(),
            formActor(),
            PublishFormCommand.builder().formId(form.formId()).build()
        );
        Long responseId = manageFormResponseUseCase.createDraft(
            form.owner(),
            formActor(),
            CreateDraftFormResponseCommand.builder().formId(form.formId()).build()
        );
        Long answerId = manageAnswerUseCase.createAnswer(
            form.owner(),
            formActor(),
            CreateAnswerCommand.builder()
                .formResponseId(responseId)
                .questionId(questionId)
                .fileIds(List.of(fileId))
                .build()
        );
        return new FormAttachment(form, responseId, answerId);
    }

    public void detachForm(FormAttachment attachment) {
        manageFormResponseUseCase.deleteDraft(
            attachment.form().owner(),
            formActor(),
            DeleteDraftFormResponseCommand.builder()
                .formResponseId(attachment.responseId())
                .build()
        );
    }

    public ChatFixture attachChat(String fileId) {
        ChatRoom room = saveChatRoomPort.save(ChatRoom.create());
        ChatRoomOwnerReference owner = ChatRoomOwnerReference.of(
            room.getId(), CHAT_NAMESPACE, "room-" + room.getId(), "default");
        saveChatRoomOwnershipPort.save(owner);
        saveChatMemberPort.save(ChatMember.of(room.getId(), MEMBER_ID));
        var message = sendChatMessageUseCase.send(new SendChatMessageCommand(
            owner,
            chatActor(),
            MessageContentType.FILE,
            null,
            List.of(fileId)
        ));
        return new ChatFixture(owner, message.messageId());
    }

    public void detachChat(ChatFixture chat) {
        deleteChatRoomUseCase.delete(chat.owner(), chatActor());
    }

    public void attachWithWrongUploader(String fileId) {
        manageFileUsageUseCase.replaceUsages(new ReplaceFileUsagesCommand(
            FileUsageCoordinate.of("e2e.wrong-owner", "1", "attachment"),
            Set.of(fileId),
            MEMBER_ID + 1
        ));
    }

    private CreateFormSectionCommand sectionCommand(Long formId) {
        return CreateFormSectionCommand.builder().formId(formId).title("section").build();
    }

    private FormActorContext formActor() {
        return FormActorContext.authenticated(MEMBER_ID);
    }

    private ChatRoomActorContext chatActor() {
        return ChatRoomActorContext.actor(MEMBER_ID);
    }

    public record UploadedFile(String fileId, String storageKey) {
    }

    public record FormFixture(Long formId, FormOwnerReference owner) {
    }

    public record FormAttachment(FormFixture form, Long responseId, Long answerId) {
    }

    public record ChatFixture(ChatRoomOwnerReference owner, Long messageId) {
    }
}
