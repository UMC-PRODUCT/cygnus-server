package com.umc.product.challenger.domain;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.common.domain.enums.OrganizationType;

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
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

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

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(nullable = false, name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "part")
    private ChallengerPart part;

    @Enumerated(EnumType.STRING)
    @Column(name = "track")
    private ChallengerTrack track;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, name = "tracks", columnDefinition = "text[]")
    private List<ChallengerTrack> tracks = new ArrayList<>();

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
        return createWithTracks(createdMemberId, gisuId, chapterId, schoolId, part,
            track == null ? List.of() : List.of(track), memberName);
    }

    public static ChallengerRecord createWithTracks(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, List<ChallengerTrack> tracks, String memberName
    ) {
        return createRecord(createdMemberId, gisuId, chapterId, schoolId, part, tracks, memberName, null, null);
    }

    public static ChallengerRecord createAdmin(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, String memberName,
        ChallengerRoleType challengerRoleType, Long organizationId
    ) {
        return createAdminWithTracks(createdMemberId, gisuId, chapterId, schoolId, part,
            List.of(), memberName, challengerRoleType, organizationId);
    }

    public static ChallengerRecord createAdminWithTracks(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, List<ChallengerTrack> tracks, String memberName,
        ChallengerRoleType challengerRoleType, Long organizationId
    ) {
        return createRecord(createdMemberId, gisuId, chapterId, schoolId, part, tracks,
            memberName, challengerRoleType, organizationId);
    }

    private static ChallengerRecord createRecord(
        Long createdMemberId, Long gisuId, Long chapterId, Long schoolId,
        ChallengerPart part, List<ChallengerTrack> tracks, String memberName,
        ChallengerRoleType challengerRoleType, Long organizationId
    ) {
        List<ChallengerTrack> selectedTracks = tracks == null ? List.of() : tracks;
        if (selectedTracks.stream().anyMatch(value -> value == null || !value.isBasic())
            || (challengerRoleType == null && part != null && !selectedTracks.isEmpty())) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        }
        ChallengerRecord record = new ChallengerRecord();
        record.code = generateUniqueCode();
        record.createdMemberId = createdMemberId;
        record.gisuId = gisuId;
        record.chapterId = chapterId;
        record.schoolId = schoolId;
        record.memberName = memberName;
        record.part = part;
        record.tracks = new ArrayList<>(new LinkedHashSet<>(selectedTracks));
        record.track = record.tracks.size() == 1 ? record.tracks.getFirst() : null;
        record.challengerRoleType = challengerRoleType;
        record.organizationId = organizationId;
        if (chapterId == null && !record.canOmitChapter()) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST,
                "수강하지 않는 중앙 운영진 코드만 지부를 생략할 수 있습니다.");
        }
        return record;
    }

    private static String generateUniqueCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return IntStream.range(0, 6)
            .map(i -> chars.charAt(CODE_RANDOM.nextInt(chars.length())))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    public List<ChallengerTrack> getTracks() {
        return List.copyOf(tracks);
    }

    public boolean isAdminRecord() {
        return this.challengerRoleType != null;
    }

    public boolean canOmitChapter() {
        return isAdminRecord() && challengerRoleType.organizationType() == OrganizationType.CENTRAL
            && tracks.isEmpty();
    }

    public void validateLearningType(GisuLearningType learningType) {
        if (tracks.stream().anyMatch(value -> value == null || !value.isBasic())) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST);
        }
        boolean valid = switch (learningType) {
            case PART -> tracks.isEmpty() && (isAdminRecord() || part != null);
            case TRACK -> isAdminRecord() || (part == null && !tracks.isEmpty())
                || (part == ChallengerPart.ADMIN && tracks.isEmpty());
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
        if (this.memberName == null || !this.memberName.equals(memberName)) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD);
        }

        if (!this.schoolId.equals(schoolId)) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD);
        }
    }
}
