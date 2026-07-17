package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.out.LoadFormOwnershipPort;
import com.umc.product.form.application.port.out.SaveFormOwnershipPort;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;

import lombok.RequiredArgsConstructor;

/** Form ownership의 immutable insert와 owner/form lock 조회를 담당하는 persistence adapter. */
@Component
@RequiredArgsConstructor
public class FormOwnershipPersistenceAdapter implements LoadFormOwnershipPort, SaveFormOwnershipPort {

    private final FormOwnershipJpaRepository formOwnershipJpaRepository;

    @Override
    @Transactional
    public FormOwnership save(FormOwnership ownership) {
        if (ownership == null) {
            throw new IllegalArgumentException("form ownership은 필수입니다.");
        }
        FormOwnerReference reference = ownership.toReference();
        int inserted = formOwnershipJpaRepository.insertIfAbsent(
            reference.formId(),
            reference.namespace(),
            reference.ownerResourceKey(),
            reference.slot()
        );
        if (inserted == 1) {
            return formOwnershipJpaRepository.findById(reference.formId())
                .orElseThrow(() -> new IllegalStateException("저장된 form ownership을 찾을 수 없습니다."));
        }

        formOwnershipJpaRepository.lockFormIdForUpdate(reference.formId());
        FormOwnership existing = formOwnershipJpaRepository.findById(reference.formId())
            .orElse(null);
        if (existing != null) {
            if (existing.matches(reference)) {
                return existing;
            }
            throw new IllegalStateException("form ownership binding은 변경할 수 없습니다.");
        }

        Optional<Long> ownerTuple = formOwnershipJpaRepository.lockOwnerTupleForUpdate(
            reference.namespace(),
            reference.ownerResourceKey(),
            reference.slot()
        );
        if (ownerTuple.isPresent()) {
            throw new IllegalStateException("form ownership owner tuple이 이미 사용 중입니다.");
        }

        throw new IllegalStateException("form ownership을 저장할 수 없습니다.");
    }

    @Transactional
    public FormOwnerReference save(FormOwnerReference reference) {
        return save(reference.toOwnership()).toReference();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormOwnership> findByFormId(Long formId) {
        return formOwnershipJpaRepository.findById(formId);
    }

    @Override
    @Transactional
    public Optional<FormOwnership> findByFormIdForUpdate(Long formId) {
        formOwnershipJpaRepository.lockFormIdForUpdate(formId);
        return formOwnershipJpaRepository.findById(formId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormOwnership> findByOwner(String namespace, String ownerResourceKey, String slot) {
        return formOwnershipJpaRepository.findByNamespaceAndOwnerResourceKeyAndSlot(
            namespace,
            ownerResourceKey,
            slot
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listDistinctNamespaces() {
        return formOwnershipJpaRepository.findDistinctNamespaces();
    }

    public long count() {
        return formOwnershipJpaRepository.count();
    }
}
