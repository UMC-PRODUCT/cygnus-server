package com.umc.product.form.application.port.in;

import com.umc.product.form.domain.FormOwnerReference;

/**
 * 새 Form의 ID가 확정된 뒤 trusted expected owner reference를 만드는 application 경계.
 *
 * <p>consumer는 server-owned namespace/owner key/slot 상수만 캡처해야 한다. HTTP/GraphQL request DTO나
 * client 입력을 이 factory로 전달하면 안 된다.</p>
 */
@FunctionalInterface
public interface FormOwnerReferenceFactory {

    FormOwnerReference create(Long savedFormId);

    static FormOwnerReferenceFactory standalone() {
        return formId -> FormOwnerReference.of(
            formId,
            "form.standalone",
            formId.toString(),
            "default"
        );
    }
}
