package com.umc.product.inquiry.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.inquiry.domain.enums.InquiryMessageSenderType;
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
@Table(name = "inquiry_message")
public class InquiryMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    // 발신자 유형 (INQUIRER / RESPONDER). 두 가지 목적으로 사용된다.
    // (1) 채팅 UI에서 말풍선 렌더링 방향 결정 (좌/우)
    // (2) RESPONDER 첫 메시지 감지 → Inquiry.startProgress() 자동 트리거
    @Column(name = "sender_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InquiryMessageSenderType senderType;

    @Column(name = "sender_member_id", nullable = false)
    private Long senderMemberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "file_metadata_ids", columnDefinition = "text[]", nullable = false)
    private List<String> fileMetadataIds;

    /**
     * 문의 메시지를 생성한다.
     */
    public static InquiryMessage create(
        Long inquiryId,
        InquiryMessageSenderType senderType,
        Long senderMemberId,
        String content
    ) {
        return InquiryMessage.builder()
            .inquiryId(inquiryId)
            .senderType(senderType)
            .senderMemberId(senderMemberId)
            .content(content)
            .fileMetadataIds(new ArrayList<>())
            .build();
    }
}
