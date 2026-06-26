package com.umc.product.inquiry.adapter.out.persistence;

import com.umc.product.inquiry.domain.Inquiry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryJpaRepository extends JpaRepository<Inquiry, Long> {

    Optional<Inquiry> findByChatRoomId(Long chatRoomId);
}
