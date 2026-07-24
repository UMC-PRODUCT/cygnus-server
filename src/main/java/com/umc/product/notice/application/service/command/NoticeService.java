package com.umc.product.notice.application.service.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.notice.application.authorization.NoticePolicyAuthorizationService;
import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.command.dto.CreateNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.DeleteNoticeCommand;
import com.umc.product.notice.application.port.in.command.dto.SendNoticeReminderCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeCommand;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.ManageNoticeTargetPort;
import com.umc.product.notice.application.port.out.SaveNoticePort;
import com.umc.product.notice.application.port.out.SaveNoticeReadPort;
import com.umc.product.notice.application.port.out.SaveNoticeTargetPort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeTarget;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NoticeService implements ManageNoticeUseCase {

    // TODO: 기획단과 PREFIX 협의 후에 수정해야 합니다.
    private static final String NOTICE_REMINDER_TITLE_PREFIX = "[⏰ 공지사항 리마인드] ";
    private static final String NOTICE_TITLE_PREFIX = "[\uD83D\uDCE2 새로운 공지사항] "; // loudspeaker emoji

    // TODO: 일단 SUFFIX는 없는걸로 ..
    private static final String REMINDER_BODY_SUFFIX = "";
    private static final String NOTICE_BODY_SUFFIX = "";

    // 도메인 내부 포트
    private final LoadNoticePort loadNoticePort;
    private final SaveNoticePort saveNoticePort;
    private final SaveNoticeTargetPort saveNoticeTargetPort;
    private final ManageNoticeTargetPort manageNoticeTargetPort;
    private final SaveNoticeReadPort saveNoticeReadPort;

    // 도메인 외부 UseCase
    private final NoticePolicyAuthorizationService policyAuthorizationService;
    private final GetChallengerUseCase getChallengerUseCase;
    private final ManageNoticeContentUseCase manageNoticeContentUseCase;
    private final RequestFcmNotificationUseCase requestFcmNotificationUseCase;

    @Override
    public List<Long> createNoticeBulk(List<CreateNoticeCommand> commands) {
        if (commands.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(commands.size());
        for (CreateNoticeCommand command : commands) {
            ids.add(createNotice(command));
        }
        return ids;
    }

    @Audited(
        domain = Domain.NOTICE,
        action = AuditAction.CREATE,
        targetType = "Notice",
        targetId = "#result",
        description = "'공지사항을 생성했습니다.'"
    )
    @Override
    public Long createNotice(CreateNoticeCommand command) {
        if (!policyAuthorizationService.evaluateCreate(command.memberId(), command.targetInfo())) {
            throw new NoticeDomainException(NoticeErrorCode.NO_WRITE_PERMISSION);
        }

        Notice notice = Notice.create(
            command.title(),
            command.content(),
            command.memberId(),
            command.shouldNotify(),
            command.mustRead()
        );

        Notice savedNotice = saveNoticePort.save(notice);

        saveNoticeTargetPort.save(NoticeTarget.builder()
            .noticeId(savedNotice.getId())
            .targetGisuId(command.targetInfo().targetGisuId())
            .targetChapterId(command.targetInfo().targetChapterId())
            .targetSchoolId(command.targetInfo().targetSchoolId())
            .targetChallengerPart(command.targetInfo().targetParts())
            .targetNoticeTab(command.targetInfo().targetNoticeTab())
            .build()
        );

        // 알람을 전송하도록 설정된 공지사항인 경우 알람 전송 시도
        if (savedNotice.isNotificationRequired()) {
            String alarmTitle = StringUtils.abbreviate(NOTICE_TITLE_PREFIX + savedNotice.getTitle(), 25);
            String alarmBody = StringUtils.abbreviate(NOTICE_BODY_SUFFIX + savedNotice.getContent(), 40);

            requestFcmNotificationUseCase.request(
                RequestFcmNotificationCommand.builder()
                    .requesterMemberId(command.memberId())
                    .targetGisuId(command.targetInfo().targetGisuId())
                    .targetChapterId(command.targetInfo().targetChapterId())
                    .targetSchoolId(command.targetInfo().targetSchoolId())
                    .targetParts(command.targetInfo().targetParts() == null
                        ? Set.of()
                        : new HashSet<>(command.targetInfo().targetParts()))
                    .title(alarmTitle)
                    .body(alarmBody)
                    .build()
            );
            savedNotice.markAsNotified(Instant.now()); // 알람 발송 완료 처리
        }

        return savedNotice.getId();
    }

    @Audited(
        domain = Domain.NOTICE,
        action = AuditAction.UPDATE,
        targetType = "Notice",
        targetId = "#command.noticeId()",
        description = "'공지사항을 수정했습니다.'"
    )
    @Override
    public void updateNoticeTitleOrContent(UpdateNoticeCommand command) {
        Notice notice = findNoticeById(command.noticeId());

        notice.updateTitleOrContent(command.title(), command.content());
        notice.updateMustRead(command.mustRead());

    }

    @Audited(
        domain = Domain.NOTICE,
        action = AuditAction.DELETE,
        targetType = "Notice",
        targetId = "#command.noticeId()",
        description = "'공지사항을 삭제했습니다.'"
    )
    @Override
    public void deleteNotice(DeleteNoticeCommand command) {
        Notice notice = findNoticeById(command.noticeId());

        // 관련 이미지, 투표, 링크 등도 모두 삭제
        manageNoticeContentUseCase.removeContentsByNoticeId(notice.getId(), command.memberId());

        // noticeRead, noticeTarget 삭제
        saveNoticeReadPort.deleteAllByNoticeId(notice.getId());
        manageNoticeTargetPort.deleteByNoticeId(notice.getId());

        // 공지 삭제
        saveNoticePort.delete(notice);
    }

    @Audited(
        domain = Domain.NOTICE,
        action = AuditAction.REMIND,
        targetType = "Notice",
        targetId = "#command.noticeId()",
        description = "'공지사항 리마인드를 발송했습니다.'"
    )
    @Override
    public void remindNotice(SendNoticeReminderCommand command) {
        Notice notice = findNoticeById(command.noticeId());

        String alarmTitle = NOTICE_REMINDER_TITLE_PREFIX + notice.getTitle();
        String alarmBody = StringUtils.abbreviate(REMINDER_BODY_SUFFIX + notice.getContent(), 40);

        // challengerId → memberId 일괄 변환 (쿼리 1회)
        Set<Long> challengerIdSet = new HashSet<>(command.targetIds());
        List<Long> memberIds = getChallengerUseCase.getAllByIds(challengerIdSet).stream()
            .map(info -> info.memberId())
            .toList();

        requestFcmNotificationUseCase.request(
            RequestFcmNotificationCommand.builder()
                .requesterMemberId(command.memberId())
                .memberIds(memberIds)
                .title(alarmTitle)
                .body(alarmBody)
                .build()
        );
    }

    @Override
    public void incrementViewCount(Long noticeId) {
        saveNoticePort.incrementViewCount(noticeId);
    }

    // === PRIVATE METHODS ===

    /**
     * Notice ID로 Entity를 조회, 없으면 Exception 발생
     */
    private Notice findNoticeById(Long noticeId) {
        return loadNoticePort.findNoticeById(noticeId)
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

}
