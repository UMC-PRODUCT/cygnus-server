package com.umc.product.form.application.port.out;

import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;

/** Form ownership binding 등록 Port. 기존 binding은 수정하지 않는다. */
public interface SaveFormOwnershipPort {

    FormOwnership save(FormOwnership ownership);

    default FormOwnerReference save(FormOwnerReference reference) {
        return save(reference.toOwnership()).toReference();
    }

    default FormOwnerReference register(FormOwnerReference reference) {
        return save(reference);
    }
}
