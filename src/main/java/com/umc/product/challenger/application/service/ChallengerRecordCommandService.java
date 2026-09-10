package com.umc.product.challenger.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerRecordPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookPlatform;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChallengerRecordCommandService implements ManageChallengerRecordUseCase {

    private final SaveChallengerRecordPort saveChallengerRecordPort;
    private final LoadChallengerRecordPort loadChallengerRecordPort;

    private final SaveChallengerPort saveChallengerPort;
    private final LoadChallengerPort loadChallengerPort;

    private final GetChapterUseCase getChapterUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final ManageChallengerRoleUseCase manageChallengerRoleUseCase;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Audited(
        domain = Domain.CHALLENGER,
        action = AuditAction.CREATE,
        targetType = "ChallengerRecord",
        targetId = "#result",
        description = "'ChallengerRecord를 생성했습니다.'"
    )
    @Override
    public Long create(CreateChallengerRecordCommand command) {
        ChallengerRecord savedRecord = saveChallengerRecordPort.save(createValidatedRecord(command));
        log.info("ChallengerRecord를 생성했습니다: recordId={}, gisuId={}, schoolId={}, chapterId={}, adminRecord={}",
            savedRecord.getId(), command.gisuId(), command.schoolId(), command.chapterId(),
            command.challengerRoleType() != null);
        return savedRecord.getId();
    }

    @Audited(
        domain = Domain.CHALLENGER,
        action = AuditAction.CREATE,
        targetType = "ChallengerRecord",
        description = "'ChallengerRecord를 대량 생성했습니다. count=' + #result.size()"
    )
    @Override
    public List<Long> createBulk(List<CreateChallengerRecordCommand> commands) {
        List<ChallengerRecord> records = commands.stream()
            .map(this::createValidatedRecord)
            .toList();

        List<ChallengerRecord> savedRecords = saveChallengerRecordPort.saveAll(records);
        log.info("ChallengerRecord를 대량 생성했습니다: count={}", savedRecords.size());
        return savedRecords.stream().map(ChallengerRecord::getId).toList();
    }

    @Audited(
        domain = Domain.CHALLENGER,
        action = AuditAction.DELETE,
        targetType = "ChallengerRecord",
        targetId = "#id",
        description = "'ChallengerRecord를 삭제했습니다.'"
    )
    @Override
    public void delete(Long id) {
        saveChallengerRecordPort.delete(loadChallengerRecordPort.getById(id));
    }

    @Audited(
        domain = Domain.CHALLENGER,
        action = AuditAction.CHECK,
        targetType = "ChallengerRecord",
        targetId = "#command.targetMemberId()",
        description = "'ChallengerRecord 코드를 사용했습니다.'"
    )
    @Override
    public void consumeCode(ConsumeChallengerRecordCommand command) {
        String code = command.code();
        Long memberId = command.targetMemberId();

        ChallengerRecord record = loadChallengerRecordPort.getByCodeForUpdate(code);
        record.validateNotUsed();

        // 운영진 기록, 즉 ChallengerRole 객체를 추가해야하는 상황이라면 Challenger를 생성하지 않음
        if (record.isAdminRecord()) {
            Long challengerId = loadChallengerPort.findByMemberIdAndGisuId(memberId, record.getGisuId())
                .orElseThrow(() -> new ChallengerDomainException(ChallengerErrorCode.NO_CHALLENGER_IN_MEMBER_GISU))
                .getId();

            MemberInfo memberInfo = getMemberUseCase.getById(memberId);

            manageChallengerRoleUseCase.createChallengerRole(
                CreateChallengerRoleCommand.builder()
                    .challengerId(challengerId)
                    .roleType(record.getChallengerRoleType())
                    .organizationId(record.getOrganizationId())
                    .responsiblePart(record.getPart())
                    .gisuId(record.getGisuId())
                    .build()
            );

            String roleType =
                record.getChallengerRoleType() != null ? record.getChallengerRoleType().name() : "역할없음";
            String responsiblePart = record.getPart() != null ? record.getPart().name() : "파트없음";

            sendWebhookAlarmUseCase.sendBuffered(
                SendWebhookAlarmCommand.builder()
                    .title("운영진 권한이 추가되었습니다.")
                    .content(
                        memberInfo.schoolName() + " " + memberInfo.nickname() + "/" + memberInfo.name() + " 님이 gisuId"
                            + record.getGisuId() + "에" + roleType + "/" + responsiblePart + " 권한을 가진 코드를 등록하였습니다.")
                    .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                    .build()
            );
        }

        // 챌린저 기록 추가하기
        else {
            record.validateLearningType(getGisuUseCase.getById(record.getGisuId()).learningType());
            MemberInfo memberInfo = getMemberUseCase.getById(memberId);
            record.validateMember(memberInfo.name(), memberInfo.schoolId());

            // 해당 기수에 챌린저 기록이 없는지 확인
            loadChallengerPort.findByMemberIdAndGisuId(memberId, record.getGisuId())
                .ifPresent(challenger -> {
                    throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS,
                        "해당 기수에 이미 챌린저 기록이 존재합니다.");
                });

            saveChallengerPort.save(
                Challenger.builder()
                    .memberId(memberId)
                    .part(record.getPart())
                    .tracks(record.getTrack() == null ? List.of() : List.of(record.getTrack()))
                    .gisuId(record.getGisuId())
                    .build()
            );
            evictAuthoritySnapshotCacheUseCase.evictByMemberId(memberId);

            sendWebhookAlarmUseCase.sendBuffered(
                SendWebhookAlarmCommand.builder()
                    .title("챌린저 기록이 추가되었습니다.")
                    .content(
                        memberInfo.schoolName() + " " + memberInfo.nickname() + "/" + memberInfo.name() + " 님이 gisuId"
                            + record.getGisuId() + "에 "
                            + (record.getTrack() == null ? record.getPart() + " 파트" : record.getTrack() + " 트랙")
                            + " 챌린저 코드를 등록하였습니다.")
                    .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                    .build()
            );
        }

        record.markAsUsed(memberId);
    }

    private ChallengerRecord createValidatedRecord(CreateChallengerRecordCommand command) {
        ChallengerRecord record = command.toEntity();
        record.validateLearningType(getGisuUseCase.getById(command.gisuId()).learningType());
        validateRecord(command.gisuId(), command.schoolId(), command.chapterId());
        return record;
    }

    private void validateRecord(Long gisuId, Long schoolId, Long chapterId) {
        ChapterInfo chapterInfo = getChapterUseCase.byGisuAndSchool(gisuId, schoolId);

        if (!chapterInfo.id().equals(chapterId)) {
            log.debug("학교 schoolId={}가 해당 기수에 속한 지부는 {}, id={} 이지만 요청에서 제공된 chapterId={} 입니다.",
                schoolId, chapterInfo.name(), chapterInfo.id(), chapterId);

            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                "주어진 학교는 해당 기수에 해당 지부에 속하지 않았습니다.");
        }
    }
}
