package com.umc.product.inquiry.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umc.product.common.BaseEntity;
import com.umc.product.inquiry.domain.enums.InquiryCategory;
import com.umc.product.inquiry.domain.enums.InquiryStatus;
import com.umc.product.inquiry.domain.enums.InquiryTarget;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "inquiry")
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private InquiryCategory category;

    @Column(name = "target", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InquiryTarget target;

    // 상태 전환은 반드시 도메인 메서드(startProgress / close / reopen)를 통해서만 가능.
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InquiryStatus status;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    // 문의 대상(target) 권한 정보. 도메인 경계를 넘는 ID 참조만 보유한다(FK 없음, nullable).
    // target=CENTRAL/PRODUCT_TEAM이면 school/chapter는 null이다. gisu는 생성 시 활성 기수가 주입된다.
    @Column(name = "target_school_id")
    private Long targetSchoolId;

    @Column(name = "target_chapter_id")
    private Long targetChapterId;

    @Column(name = "target_gisu_id")
    private Long targetGisuId;

    // 수정/삭제는 정책상 전면 불가
    @Column(name = "author_member_id", nullable = false)
    private Long authorMemberId;

    // 담당 운영진 목록. 다수 지정 가능
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "assigned_member_ids", columnDefinition = "bigint[]", nullable = false)
    private List<Long> assignedMemberIds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "file_metadata_ids", columnDefinition = "text[]", nullable = false)
    private List<String> fileMetadataIds;

    // 운영진 중 누군가 한 명이라도 열람했는지 여부. 채팅방 단위로 읽음 상태 관리
    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    /**
     * 문의사항을 생성한다. 초기 status는 RECEIVED, isRead는 false로 고정된다.
     */
    public static Inquiry create(
        String title,
        String content,
        InquiryCategory category,
        InquiryTarget target,
        Long chatRoomId,
        Long authorMemberId,
        Long targetSchoolId,
        Long targetChapterId,
        Long targetGisuId
    ) {
        return Inquiry.builder()
            .title(title)
            .content(content)
            .category(category)
            .target(target)
            .status(InquiryStatus.RECEIVED)
            .chatRoomId(chatRoomId)
            .authorMemberId(authorMemberId)
            .targetSchoolId(targetSchoolId)
            .targetChapterId(targetChapterId)
            .targetGisuId(targetGisuId)
            .assignedMemberIds(new ArrayList<>())
            .fileMetadataIds(new ArrayList<>())
            .isRead(false)
            .build();
    }

    /**
     * 담당 운영진을 지정한다. 이미 지정된 운영진이면 무시한다.
     */
    public void assignManager(Long memberId) {
        if (!this.assignedMemberIds.contains(memberId)) {
            this.assignedMemberIds.add(memberId);
        }
    }

    /**
     * 담당 운영진 지정을 해제한다.
     */
    public void unassignManager(Long memberId) {
        this.assignedMemberIds.remove(memberId);
    }

    /**
     * 담당 운영진을 이관한다. 기존 담당자를 목록에서 제거하고 새 담당자를 추가한다.
     * <p>
     * 기존 담당자가 목록에 없어도 무시하며, 새 담당자가 이미 있으면 중복 추가하지 않는다. 상태(status)는 변경하지 않는다.
     */
    public void transferManager(Long fromMemberId, Long toMemberId) {
        this.assignedMemberIds.remove(fromMemberId);
        if (!this.assignedMemberIds.contains(toMemberId)) {
            this.assignedMemberIds.add(toMemberId);
        }
    }

    /**
     * 운영진이 처음 문의를 열람할 때 호출된다. 이미 true이면 아무 작업도 하지 않는다
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
        }
    }

    /**
     * 열람 상태를 미열람으로 되돌린다.
     */
    public void markAsUnread() {
        this.isRead = false;
    }

    /**
     * 해당 멤버가 이 문의에 접근할 수 있는지 판정한다.
     * <p>
     * 접근 가능 조건: 작성자이거나 운영진이다. 운영진 여부는 {@code LoadOperatorStatusPort}로 판정한
     * 결과를 호출자가 주입한다.
     *
     * @param memberId   접근을 시도하는 멤버 ID
     * @param isOperator 해당 멤버의 운영진 여부
     */
    public boolean isAccessibleBy(Long memberId, boolean isOperator) {
        return this.authorMemberId.equals(memberId) || isOperator;
    }

    /**
     * RECEIVED → IN_PROGRESS 전환. RESPONDER의 첫 메시지 전송 시 자동 호출된다.
     */
    public void startProgress() {
        if (this.status != InquiryStatus.RECEIVED) {
            throw new InquiryDomainException(InquiryErrorCode.INQUIRY_INVALID_STATUS_FOR_PROGRESS);
        }
        this.status = InquiryStatus.IN_PROGRESS;
    }

    /**
     * RECEIVED 또는 IN_PROGRESS → CLOSED 전환. 운영진이 명시적으로 호출한다..
     */
    public void close() {
        if (this.status == InquiryStatus.CLOSED) {
            throw new InquiryDomainException(InquiryErrorCode.INQUIRY_ALREADY_CLOSED);
        }
        this.status = InquiryStatus.CLOSED;
    }

    /**
     * CLOSED → IN_PROGRESS 전환.
     * <p>
     * 카카오톡 채널 방식을 따름: 문의자/운영진 구분 없이 메시지 전송 시 자동 호출. 수동 재오픈 API는 제공하지 않으며, SendInquiryMessageUseCase에서 처리한다.
     */
    public void reopen() {
        if (this.status != InquiryStatus.CLOSED) {
            throw new InquiryDomainException(InquiryErrorCode.INQUIRY_INVALID_STATUS_FOR_REOPEN);
        }
        this.status = InquiryStatus.IN_PROGRESS;
        markAsUnread();
    }
}
