package com.umc.product.form.domain;

import com.umc.product.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "question_option")
public class QuestionOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    // 길이 변경 시 AnswerChoice.answeredAsContent도 함께 변경
    @Column(nullable = false, length = 500)
    private String content; // 보기 내용 (예: '남자', '여자')

    @Column(name = "order_no", nullable = false)
    private Long orderNo; // 보기 순서

    @Column(name = "is_other", nullable = false)
    private boolean isOther;

    public static QuestionOption create(String content, long orderNo, boolean isOther) {
        return QuestionOption.builder()
            .content(content)
            .orderNo(orderNo)
            .isOther(isOther)
            .build();
    }

    /**
     * 선택지 속성 부분 업데이트.
     * null 인 필드는 기존 값 유지.
     */
    public void update(String content, Boolean isOther) {
        if (content != null) {
            this.content = content;
        }
        if (isOther != null) {
            this.isOther = isOther;
        }
    }

    public void updateOrderNo(long orderNo) {
        this.orderNo = orderNo;
    }

    public void assignTo(Question question) {
        this.question = question;
    }

}
