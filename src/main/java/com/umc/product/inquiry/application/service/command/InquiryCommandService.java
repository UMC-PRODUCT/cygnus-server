package com.umc.product.inquiry.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.MarkChatRoomReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;
import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessagesForAuthorizedCallerUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesForAuthorizedCallerQuery;
import com.umc.product.inquiry.application.port.in.command.AssignInquiryManagerUseCase;
import com.umc.product.inquiry.application.port.in.command.CloseInquiryUseCase;
import com.umc.product.inquiry.application.port.in.command.MarkInquiryReadUseCase;
import com.umc.product.inquiry.application.port.in.command.SubmitInquiryUseCase;
import com.umc.product.inquiry.application.port.in.command.TransferInquiryManagerUseCase;
import com.umc.product.inquiry.application.port.in.command.dto.AssignInquiryManagerCommand;
import com.umc.product.inquiry.application.port.in.command.dto.CloseInquiryCommand;
import com.umc.product.inquiry.application.port.in.command.dto.MarkInquiryReadCommand;
import com.umc.product.inquiry.application.port.in.command.dto.SubmitInquiryCommand;
import com.umc.product.inquiry.application.port.in.command.dto.TransferInquiryManagerCommand;
import com.umc.product.inquiry.application.port.in.query.dto.InquiryInfo;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.SaveInquiryPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 문의 생성 및 운영진 관리(담당자 지정/이관, 종료) 흐름을 단일 트랜잭션으로 엮는다.
 * <p>
 * 생성: 작성자가 선택한 target과 (SCHOOL이면 학교 / CHAPTER면 지부)을 검증한 뒤, 활성 기수를 자동 주입하고 채팅방을 생성하여 문의를 저장한다.
 * 저장된 target_* 컬럼은 이후 운영진 판정(LoadOperatorStatusAdapter)의 근거가 된다.
 * <p>
 * 관리(지정/이관/종료): 요청 주체가 해당 문의의 운영진인지 판정한 뒤 도메인 메서드에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InquiryCommandService implements
    SubmitInquiryUseCase,
    AssignInquiryManagerUseCase,
    TransferInquiryManagerUseCase,
    CloseInquiryUseCase,
    MarkInquiryReadUseCase {

    private final SaveInquiryPort saveInquiryPort;
    private final GetGisuUseCase getGisuUseCase;
    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final JoinChatRoomUseCase joinChatRoomUseCase;
    private final CheckChatRoomAccessUseCase checkChatRoomAccessUseCase;
    private final MarkChatRoomReadUseCase markChatRoomReadUseCase;
    private final GetChatMessagesForAuthorizedCallerUseCase getChatMessagesForAuthorizedCallerUseCase;
    private final LoadInquiryPort loadInquiryPort;
    private final LoadOperatorStatusPort loadOperatorStatusPort;

    @Override
    public InquiryInfo submit(SubmitInquiryCommand command) {
        InquiryTarget target = command.target();

        // 1) target별 필수값 검증. CENTRAL/PRODUCT_TEAM은 school/chapter 입력을 무시(null로 저장)한다.
        Long resolvedSchoolId = null;
        Long resolvedChapterId = null;
        switch (target) {
            case SCHOOL -> {
                if (command.targetSchoolId() == null) {
                    throw new InquiryDomainException(InquiryErrorCode.INQUIRY_TARGET_INFO_REQUIRED,
                        "SCHOOL 문의는 대상 학교(targetSchoolId)가 필요합니다.");
                }
                resolvedSchoolId = command.targetSchoolId();
            }
            case CHAPTER -> {
                if (command.targetChapterId() == null) {
                    throw new InquiryDomainException(InquiryErrorCode.INQUIRY_TARGET_INFO_REQUIRED,
                        "CHAPTER 문의는 대상 지부(targetChapterId)가 필요합니다.");
                }
                resolvedChapterId = command.targetChapterId();
            }
            case CENTRAL, PRODUCT_TEAM -> {
                // school/chapter 입력 무시 → null 유지
            }
        }

        // 2) 활성 기수 자동 주입
        Long gisuId = getGisuUseCase.getActiveGisuId();

        // 3) 채팅방 생성 — 문의는 chatRoom 위에 얹힌다(Inquiry.chatRoomId NOT NULL). 작성자를 생성자로 방을 만든다.
        Long chatRoomId = createChatRoomUseCase.create(
            CreateChatRoomCommand.from(command.authorMemberId())).roomId();

        // 4) 문의 생성 및 저장
        Inquiry inquiry = Inquiry.create(
            command.title(),
            command.content(),
            command.category(),
            target,
            chatRoomId,
            command.authorMemberId(),
            resolvedSchoolId,
            resolvedChapterId,
            gisuId
        );

        return InquiryInfo.from(saveInquiryPort.save(inquiry), 0L);
    }

    @Override
    public void assign(AssignInquiryManagerCommand command) {
        Inquiry inquiry = loadAndAuthorize(command.inquiryId(), command.actorMemberId());
        inquiry.assignManager(command.targetManagerId());
        saveInquiryPort.save(inquiry);
        // 지정된 운영진을 채팅방 멤버로 등록 (이미 멤버면 중복 등록 생략)
        joinIfNotMember(inquiry.getChatRoomId(), command.targetManagerId());
    }

    @Override
    public void transfer(TransferInquiryManagerCommand command) {
        Inquiry inquiry = loadAndAuthorize(command.inquiryId(), command.actorMemberId());
        inquiry.transferManager(command.fromManagerId(), command.toManagerId());
        saveInquiryPort.save(inquiry);
    }

    @Override
    public void close(CloseInquiryCommand command) {
        Inquiry inquiry = loadAndAuthorize(command.inquiryId(), command.actorMemberId());
        // 이미 CLOSED면 close()가 INQUIRY_ALREADY_CLOSED를 던진다(도메인 위임).
        inquiry.close();
        saveInquiryPort.save(inquiry);
    }

    /**
     * 문의 읽음 처리. 작성자와 운영진 모두 호출할 수 있다(assign/transfer/close 와 달리 운영진 전용이 아니다).
     * <p>
     * inquiry 의 {@code isRead} 는 "운영진이 열람했는가" 플래그이므로 운영진 호출에서만 갱신한다. 작성자가 읽어도
     * 운영진 열람 여부는 바뀌지 않아야 하기 때문이다.
     * <p>
     * 접근 권한은 이 메서드에서 판정하므로, 채팅 메시지 조회는 membership 검사가 없는 권한위임형 유스케이스를 사용한다.
     * 담당자로 지정되지 않아 채팅방 멤버가 아닌 운영진도 읽음 처리를 할 수 있어야 한다.
     */
    @Override
    public void markRead(MarkInquiryReadCommand command) {
        Inquiry inquiry = loadInquiryPort.getById(command.inquiryId());
        boolean isOperator = loadOperatorStatusPort.isOperator(
            LoadOperatorStatusContext.of(command.memberId(), inquiry));
        if (!inquiry.isAccessibleBy(command.memberId(), isOperator)) {
            throw new InquiryDomainException(InquiryErrorCode.NO_INQUIRY_PERMISSION);
        }

        if (isOperator) {
            inquiry.markAsRead();
        }

        Long chatRoomId = inquiry.getChatRoomId();
        // 엔진 markRead 는 "lastSeenMessageId 까지 읽음"이므로, 방의 최신(id DESC 1건) 메시지 id 를 넘기면 기존의 "방 전체 읽음"과 동치가 된다.
        ChatMessageCursorResult latest = getChatMessagesForAuthorizedCallerUseCase.getMessages(
            new GetChatMessagesForAuthorizedCallerQuery(chatRoomId, null, 1));
        if (!latest.content().isEmpty()) {
            // 메시지가 없는 빈 방이면 채팅방 읽음 처리 대상이 없으므로 생략한다(inquiry 저장은 그대로 진행).
            Long lastSeenMessageId = latest.content().get(0).messageId();
            markChatRoomReadUseCase.markRead(
                MarkChatRoomReadCommand.of(chatRoomId, command.memberId(), lastSeenMessageId));
        }

        saveInquiryPort.save(inquiry);
    }

    /**
     * 문의를 로드하고 요청 주체가 해당 문의의 운영진인지 판정한다. 운영진이 아니면 NO_INQUIRY_PERMISSION 예외를 던진다.
     */
    private Inquiry loadAndAuthorize(Long inquiryId, Long actorMemberId) {
        Inquiry inquiry = loadInquiryPort.getById(inquiryId);
        boolean isOperator = loadOperatorStatusPort.isOperator(
            LoadOperatorStatusContext.of(actorMemberId, inquiry));
        if (!isOperator) {
            throw new InquiryDomainException(InquiryErrorCode.NO_INQUIRY_PERMISSION);
        }
        return inquiry;
    }

    /**
     * 이미 채팅방 멤버면 생략하고, 멤버가 아닐 때만 join한다.
     * joinChatRoom()은 멱등하지 않아(이미 멤버면 예외) 사전 체크가 필수다.
     */
    private void joinIfNotMember(Long chatRoomId, Long memberId) {
        if (!checkChatRoomAccessUseCase.hasChatRoomAccess(memberId, chatRoomId)) {
            joinChatRoomUseCase.joinChatRoom(new JoinChatRoomCommand(chatRoomId, memberId));
        }
    }
}
