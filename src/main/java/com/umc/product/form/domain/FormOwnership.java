package com.umc.product.form.domain;

import org.hibernate.annotations.Immutable;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Form 하나의 ownership binding을 보관하는 insert-only entity.
 *
 * <p>Form aggregate와의 객체 연관을 만들지 않고 scalar ID와 FK만 둔다. ownership은
 * authorization truth이므로 binding을 변경하는 setter/update API를 제공하지 않는다.</p>
 */
@Entity
@Immutable
@Table(
    name = "form_ownership",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_form_ownership_owner_tuple",
        columnNames = {"namespace", "owner_resource_key", "slot"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormOwnership extends BaseEntity {

    @Id
    @Column(name = "form_id", nullable = false, updatable = false)
    private Long formId;

    @Column(nullable = false, length = FormOwnerReference.NAMESPACE_MAX_LENGTH, updatable = false)
    private String namespace;

    @Column(name = "owner_resource_key", nullable = false,
        length = FormOwnerReference.OWNER_RESOURCE_KEY_MAX_LENGTH, updatable = false)
    private String ownerResourceKey;

    @Column(nullable = false, length = FormOwnerReference.SLOT_MAX_LENGTH, updatable = false)
    private String slot;

    private FormOwnership(FormOwnerReference reference) {
        this.formId = reference.formId();
        this.namespace = reference.namespace();
        this.ownerResourceKey = reference.ownerResourceKey();
        this.slot = reference.slot();
    }

    public static FormOwnership from(FormOwnerReference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("form owner reference는 필수입니다.");
        }
        return new FormOwnership(reference);
    }

    public static FormOwnership of(
        Long formId,
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        return from(FormOwnerReference.of(formId, namespace, ownerResourceKey, slot));
    }

    public FormOwnerReference toReference() {
        return FormOwnerReference.of(formId, namespace, ownerResourceKey, slot);
    }

    public boolean matches(FormOwnerReference reference) {
        return reference != null && toReference().sameBinding(reference);
    }

    public boolean sameBinding(FormOwnership other) {
        return other != null && toReference().sameBinding(other.toReference());
    }
}
