package com.umc.product.form.application.port.out;

import java.util.List;

import com.umc.product.form.application.port.out.dto.FormOwnershipBackfillMapping;
import com.umc.product.form.domain.FormOwnerReference;

public interface FormOwnershipBackfillSource {

    String sourceName();

    String namespace();

    String slot();

    default void preflight() {
    }

    List<FormOwnershipBackfillMapping> loadBatch(long afterParentId, int batchSize);

    default FormOwnerReference toOwnerReference(FormOwnershipBackfillMapping mapping) {
        return FormOwnerReference.of(
            mapping.formId(),
            namespace(),
            mapping.ownerResourceKey(),
            slot()
        );
    }
}
