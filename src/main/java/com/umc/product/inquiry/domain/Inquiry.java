package com.umc.product.inquiry.domain;

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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
        Long authorMemberId
    ) {
        return Inquiry.builder()
            .title(title)
            .content(content)
            .category(category)
            .target(target)
            .status(InquiryStatus.RECEIVED)
            .authorMemberId(authorMemberId)
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
     * 운영진이 처음 문의를 열람할 때 호출된다. 이미 true이면 아무 작업도 하지 않는다
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
        }
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
        this.isRead = false;
    }
}
