package com.umc.product.form.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "form_section")
public class FormSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "order_no")
    private Long orderNo;

    public static FormSection create(Form form, String title, String description, Long orderNo) {
        return FormSection.builder()
            .form(form)
            .title(title)
            .description(description)
            .orderNo(orderNo)
            .build();
    }

    /**
     * 섹션 메타데이터 부분 업데이트.
     * null 인 필드는 기존 값 유지.
     */
    public void update(String title, String description) {
        update(title, description, false);
    }

    public void update(String title, String description, boolean clearDescription) {
        if (title != null) {
            this.title = title;
        }
        if (clearDescription) {
            this.description = null;
        } else if (description != null) {
            this.description = description;
        }
    }

    public void updateOrderNo(Long orderNo) {
        this.orderNo = orderNo;
    }
}
