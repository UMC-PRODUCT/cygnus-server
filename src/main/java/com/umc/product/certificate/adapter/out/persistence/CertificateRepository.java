package com.umc.product.certificate.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateType;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    List<Certificate> findAllByRecipientMemberIdOrderByIssuedAtDescIdDesc(Long recipientMemberId);

    @Query("""
        SELECT c
        FROM Certificate c
        WHERE c.type = :type
          AND c.issuer = :issuer
          AND c.recipientMemberId = :recipientMemberId
          AND c.gisuId = :gisuId
          AND c.status = :status
          AND c.expiresAt > :now
          AND ((:projectId IS NULL AND c.projectId IS NULL) OR c.projectId = :projectId)
          AND ((:meritTitle IS NULL AND c.meritTitle IS NULL) OR c.meritTitle = :meritTitle)
        ORDER BY c.issuedAt DESC, c.id DESC
        """)
    List<Certificate> findValidByScope(
        @Param("type") CertificateType type,
        @Param("issuer") CertificateIssuer issuer,
        @Param("recipientMemberId") Long recipientMemberId,
        @Param("gisuId") Long gisuId,
        @Param("projectId") Long projectId,
        @Param("meritTitle") String meritTitle,
        @Param("status") CertificateStatus status,
        @Param("now") Instant now
    );
}
