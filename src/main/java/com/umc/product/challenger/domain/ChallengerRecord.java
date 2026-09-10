package com.umc.product.challenger.domain;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "challenger_record")
public class ChallengerRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 6)
    private String code;

    @Column(name = "member_name", length = 30)
    private String memberName;

    @Column(nullable = false, name = "created_member_id")
    private Long createdMemberId;

    @Column(nullable = false, name = "gisu_id")
    private Long gisuId;

    @Column(nullable = false, name = "chapter_id")
    private Long chapterId;

    @Column(nullable = false, name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "part")
    private ChallengerPart part;

    @Enumerated(EnumType.STRING)
    @Column(name = "track")
    private ChallengerTrack track;

    @Column(name = "challenger_role_type")
    @Enumerated(EnumType.STRING)
    private ChallengerRoleType challengerRoleType;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(nullable = false, name = "is_used")
    private boolean isUsed;

    @Column(name = "used_member_id")
    private Long usedMemberId;

    @Column(name = "used_at")
    private Instant usedAt;

    public static ChallengerRecord create(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, String memberName
    ) {
        return create(createdMemberId, gisuId, chapterId, schoolId, part, null, memberName);
    }

    public static ChallengerRecord create(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, ChallengerTrack track, String memberName
    ) {
        if (track != null && (part != null || !track.isBasic())) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        }
        ChallengerRecord record = new ChallengerRecord();

        record.code = generateUniqueCode();
        record.createdMemberId = createdMemberId;
        record.part = part;
        record.track = track;
        record.gisuId = gisuId;
        record.schoolId = schoolId;
        record.chapterId = chapterId;
        record.memberName = memberName;
        record.isUsed = false; // 생성 시에는 사용되지 않은 상태로 시작

        return record;
    }

    public static ChallengerRecord createAdmin(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, String memberName,
        ChallengerRoleType challengerRoleType, Long organizationId
    ) {
        // 이전 팩토리 메소드 재사용
        ChallengerRecord record = create(createdMemberId, gisuId, chapterId, schoolId, part, memberName);

        record.challengerRoleType = challengerRoleType;
        record.organizationId = organizationId;

        return record;
    }

    private static String generateUniqueCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return IntStream.range(0, 6)
            .map(i -> chars.charAt(random.nextInt(chars.length())))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    public boolean isAdminRecord() {
        return this.challengerRoleType != null;
    }

    public void validateLearningType(GisuLearningType learningType) {
        if (isAdminRecord()) {
            return;
        }
        boolean valid = switch (learningType) {
            case PART -> part != null && track == null;
            case TRACK -> part == null && track != null && track.isBasic();
        };
        if (!valid) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                "기수의 학습 유형에 맞는 파트 또는 기본 트랙을 선택해주세요.");
        }
    }

    public void markAsUsed(Long usedMemberId) {
        validateNotUsed();

        this.isUsed = true;
        this.usedMemberId = usedMemberId;
        this.usedAt = Instant.now();
    }

    public void validateNotUsed() {
        if (this.isUsed) {
            throw new ChallengerDomainException(ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE);
        }
    }

    public void validateMember(String memberName, Long schoolId) {
        if (!this.memberName.equals(memberName)) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD);
        }

        if (!this.schoolId.equals(schoolId)) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD);
        }
    }
}
