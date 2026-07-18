package com.umc.product.notice.adapter.out.backfill;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.FormOwnershipNamespaceDeclaration;
import com.umc.product.notice.application.policy.NoticeVoteOwnerReferenceFactory;

@Component
public class NoticeVoteFormOwnershipNamespaceDeclaration
    implements FormOwnershipNamespaceDeclaration {

    @Override
    public String namespace() {
        return NoticeVoteOwnerReferenceFactory.NAMESPACE;
    }
}
