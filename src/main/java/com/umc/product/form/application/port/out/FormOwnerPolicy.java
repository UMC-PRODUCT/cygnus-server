package com.umc.product.form.application.port.out;

import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * Form consumer가 제공하는 namespace별 business permission 정책 SPI.
 *
 * <p>Form engine은 consumer aggregate나 타입을 참조하지 않고 이 계약만 호출한다.</p>
 */
public interface FormOwnerPolicy {

    String namespace();

    boolean allows(FormOwnerReference ownerReference, FormOperation operation, Long memberId);
}
