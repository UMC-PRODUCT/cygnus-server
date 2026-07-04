package com.umc.product.form.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.form.domain.enums.FormResponseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "form_response")
public class FormResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Column(name = "respondent_member_id", nullable = false)
    private Long respondentMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FormResponseStatus status = FormResponseStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_ip")
    private String submittedIp;

    @Column(name = "last_saved_at", nullable = false)
    private Instant lastSavedAt;

    public static FormResponse createDraft(Form form, Long respondentMemberId) {
        FormResponse fr = new FormResponse();
        fr.form = form;
        fr.respondentMemberId = respondentMemberId;
        fr.status = FormResponseStatus.DRAFT;
        fr.lastSavedAt = Instant.now();
        return fr;
    }

    public void submit(Instant submittedAt, String submittedIp) {
        if (this.status == FormResponseStatus.SUBMITTED) {
            return;
        }
        this.status = FormResponseStatus.SUBMITTED;
        this.submittedAt = submittedAt;
        this.submittedIp = submittedIp;
    }

    public void updateLastSavedAt(Instant now) {
        this.lastSavedAt = now;
    }

}
