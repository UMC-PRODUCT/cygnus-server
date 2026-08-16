package com.umc.product.form.application.port.out;

import com.umc.product.form.domain.Form;

public interface SaveFormPort {
    Form save(Form form);

    void deleteById(Long formId);
}
