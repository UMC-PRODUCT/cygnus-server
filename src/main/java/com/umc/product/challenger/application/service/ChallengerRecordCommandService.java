package com.umc.product.challenger.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
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
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.command.LockMemberUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookPlatform;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
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
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final LockMemberUseCase lockMemberUseCase;
    private final ListChallengerRoleUseCase listChallengerRoleUseCase;
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
        Long memberId = command.targetMemberId();
        // 서로 다른 코드도 회원 단위로 직렬화한 뒤 최신 소속과 수강 목록을 조회한다.
        lockMemberUseCase.lockById(memberId);
        ChallengerRecord record = loadChallengerRecordPort.getByCodeForUpdate(command.code());
        record.validateNotUsed();

        GisuLearningType learningType = getGisuUseCase.getById(record.getGisuId()).learningType();
        record.validateLearningType(learningType);
        MemberInfo memberInfo = getMemberUseCase.getById(memberId);
        record.validateMember(memberInfo.name(), memberInfo.schoolId());
        validateRecord(record);

        Optional<Challenger> existing = loadChallengerPort.findByMemberIdAndGisuId(memberId, record.getGisuId());
        if (learningType == GisuLearningType.PART) {
            if (record.isAdminRecord() && existing.isEmpty()) {
                throw new ChallengerDomainException(ChallengerErrorCode.NO_CHALLENGER_IN_MEMBER_GISU);
            }
            if (!record.isAdminRecord() && existing.isPresent()) {
                throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS);
            }
        } else {
            existing.ifPresent(Challenger::validateChallengerStatus);
        }

        boolean membershipAdded = existing.isEmpty();
        Challenger challenger = existing.orElseGet(() -> newMembership(record, memberId, learningType));
        boolean tracksAdded = false;
        for (var track : record.getTracks()) {
            tracksAdded |= challenger.addTrack(track);
        }
        boolean roleAdded = record.isAdminRecord() && !hasRole(record, memberId);
        if (!membershipAdded && !tracksAdded && !roleAdded) {
            throw new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS,
                "코드의 수강과 운영진 역할이 모두 이미 등록되어 있습니다.");
        }

        if (membershipAdded || tracksAdded) {
            challenger = saveChallengerPort.save(challenger);
        }
        if (roleAdded) {
            manageChallengerRoleUseCase.createChallengerRole(CreateChallengerRoleCommand.builder()
                .challengerId(challenger.getId())
                .roleType(record.getChallengerRoleType())
                .organizationId(record.getOrganizationId())
                .responsiblePart(record.getPart())
                .gisuId(record.getGisuId())
                .build());
        }
        record.markAsUsed(memberId);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(memberId);
        sendWebhookAlarmUseCase.sendBuffered(SendWebhookAlarmCommand.builder()
            .title("챌린저 코드가 등록되었습니다.")
            .content(memberInfo.schoolName() + " " + memberInfo.nickname() + "/" + memberInfo.name()
                + " 님이 gisuId=" + record.getGisuId() + "의 코드를 등록했습니다."
                + " tracks=" + record.getTracks() + ", role=" + record.getChallengerRoleType())
            .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
            .build());
    }

    private Challenger newMembership(ChallengerRecord record, Long memberId, GisuLearningType learningType) {
        if (learningType == GisuLearningType.TRACK) {
            return Challenger.createWithoutEnrollment(memberId, record.getGisuId());
        }
        return Challenger.builder().memberId(memberId).gisuId(record.getGisuId()).part(record.getPart()).build();
    }

    private boolean hasRole(ChallengerRecord record, Long memberId) {
        return listChallengerRoleUseCase.listByMemberIdAndGisuId(memberId, record.getGisuId()).stream()
            .anyMatch(role -> role.roleType() == record.getChallengerRoleType()
                && Objects.equals(role.organizationId(), record.getOrganizationId())
                && (record.getChallengerRoleType() != ChallengerRoleType.SCHOOL_PART_LEADER
                    || role.responsiblePart() == record.getPart()));
    }

    private ChallengerRecord createValidatedRecord(CreateChallengerRecordCommand command) {
        ChallengerRecord record = command.toEntity();
        record.validateLearningType(getGisuUseCase.getById(command.gisuId()).learningType());
        validateRecord(record);
        return record;
    }

    private void validateRecord(ChallengerRecord record) {
        if (record.getChapterId() == null) {
            if (record.canOmitChapter()) {
                getSchoolUseCase.getSchoolDetail(record.getSchoolId());
                return;
            }
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                "수강 없는 중앙 운영진만 지부를 생략할 수 있습니다.");
        }
        Long gisuId = record.getGisuId();
        Long schoolId = record.getSchoolId();
        Long chapterId = record.getChapterId();
        ChapterInfo chapterInfo = getChapterUseCase.byGisuAndSchool(gisuId, schoolId);

        if (!chapterInfo.id().equals(chapterId)) {
            log.debug("학교 schoolId={}가 해당 기수에 속한 지부는 {}, id={} 이지만 요청에서 제공된 chapterId={} 입니다.",
                schoolId, chapterInfo.name(), chapterInfo.id(), chapterId);

            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                "주어진 학교는 해당 기수에 해당 지부에 속하지 않았습니다.");
        }
    }
}
