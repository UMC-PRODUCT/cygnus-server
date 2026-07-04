package com.umc.product.form.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.umc.product.form.domain.Form;

public interface LoadFormPort {
    Optional<Form> findById(Long formId);

    List<Form> batchGetByIds(Collection<Long> formIds);
}
