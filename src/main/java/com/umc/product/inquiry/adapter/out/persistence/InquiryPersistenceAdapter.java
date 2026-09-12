package com.umc.product.inquiry.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.SaveInquiryPort;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InquiryPersistenceAdapter implements SaveInquiryPort, LoadInquiryPort {

    private final InquiryJpaRepository inquiryJpaRepository;
    private final InquiryQueryRepository inquiryQueryRepository;

    @Override
    public Inquiry save(Inquiry inquiry) {
        return inquiryJpaRepository.save(inquiry);
    }

    @Override
    public Inquiry getById(Long inquiryId) {
        return inquiryJpaRepository.findById(inquiryId)
            .orElseThrow(() -> new InquiryDomainException(InquiryErrorCode.INQUIRY_NOT_FOUND));
    }

    @Override
    public Inquiry getByRoomId(Long chatRoomId) {
        return inquiryJpaRepository.findByChatRoomId(chatRoomId)
            .orElseThrow(() -> new InquiryDomainException(InquiryErrorCode.INQUIRY_NOT_FOUND));
    }

    @Override
    public List<Inquiry> listByScope(InquiryAccessScope scope, GetInquiryListQuery filter) {
        return inquiryQueryRepository.listByScope(scope, filter);
    }
}
