package com.umc.product.inquiry.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.inquiry.domain.Inquiry;

public interface InquiryJpaRepository extends JpaRepository<Inquiry, Long> {
}
