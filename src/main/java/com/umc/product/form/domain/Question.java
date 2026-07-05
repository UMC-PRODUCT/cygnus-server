package com.umc.product.form.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.form.domain.enums.QuestionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "question")
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_section_id", nullable = false)
    private FormSection formSection;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @Column(name = "order_no", nullable = false)
    private Long orderNo;

    @Column(name = "parent_question_id")
    private Long parentQuestionId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public static Question create(
        String title,
        QuestionType type,
        boolean isRequired,
        long orderNo
    ) {
        return create(title, null, type, isRequired, orderNo);
    }

    public static Question create(
        String title,
        String description,
        QuestionType type,
        boolean isRequired,
        long orderNo
    ) {
        return Question.builder()
            .title(title)
            .description(description)
            .type(type)
            .isRequired(isRequired)
            .orderNo(orderNo)
            .isActive(true)
            .build();
    }

    /**
     * 기존 질문을 원본으로 새 질문을 생성한다 (Copy-on-Write).
     * 원본 질문에 이미 응답이 존재할 때 호출하며, 호출 측에서 origin.deactivate() 를 함께 수행해야 한다.
     */
    public static Question fork(Question origin) {
        return Question.builder()
            .title(origin.title)
            .description(origin.description)
            .type(origin.type)
            .isRequired(origin.isRequired)
            .orderNo(origin.orderNo)
            .parentQuestionId(origin.id)
            .isActive(true)
            .build();
    }

    public void assignTo(FormSection section) {
        this.formSection = section;
    }

    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 질문 속성 부분 업데이트 (PATCH semantics).
     * null 인 필드는 기존 값 유지. type 변경은 별도 {@link #changeType} 사용.
     */
    public void update(String title, String description, Boolean isRequired) {
        update(title, description, isRequired, false);
    }

    public void update(String title, String description, Boolean isRequired, boolean clearDescription) {
        if (title != null) {
            this.title = title;
        }
        if (clearDescription) {
            this.description = null;
        } else if (description != null) {
            this.description = description;
        }
        if (isRequired != null) {
            this.isRequired = isRequired;
        }
    }

    /**
     * 질문 타입 변경. 호출 측 Service 가 옵션 / 응답 cascade 정리 책임.
     */
    public void changeType(QuestionType type) {
        this.type = type;
    }

    public void updateOrderNo(long orderNo) {
        this.orderNo = orderNo;
    }

}
