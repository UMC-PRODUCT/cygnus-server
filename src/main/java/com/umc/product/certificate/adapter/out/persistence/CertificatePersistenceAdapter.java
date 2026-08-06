package com.umc.product.certificate.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.certificate.application.port.out.LoadCertificatePort;
import com.umc.product.certificate.application.port.out.LockCertificateIssuancePort;
import com.umc.product.certificate.application.port.out.SaveCertificatePort;
import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CertificatePersistenceAdapter implements LoadCertificatePort, SaveCertificatePort, LockCertificateIssuancePort {

    private final CertificateRepository certificateRepository;
    private final EntityManager entityManager;

    @Override
    public void lockScope(
        CertificateTemplate template,
        Long recipientMemberId,
        Long gisuId,
        String meritTitle
    ) {
        String scopeKey = template.name()
            + '|' + recipientMemberId
            + '|' + gisuId
            + '|' + (meritTitle == null ? "-1:" : meritTitle.length() + ":" + meritTitle);
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:scopeKey, 0))")
            .setParameter("scopeKey", scopeKey)
            .getSingleResult();
    }

    @Override
    public Optional<Certificate> findById(Long certificateId) {
        return certificateRepository.findById(certificateId);
    }

    @Override
    public Certificate getById(Long certificateId) {
        return certificateRepository.findById(certificateId)
            .orElseThrow(() -> new CertificateException(CertificateErrorCode.CERTIFICATE_NOT_FOUND));
    }

    @Override
    public Optional<Certificate> findBySerialNumber(String serialNumber) {
        return certificateRepository.findBySerialNumber(serialNumber);
    }

    @Override
    public Optional<Certificate> findValidByScope(
        CertificateTemplate template,
        Long recipientMemberId,
        Long gisuId,
        String meritTitle,
        Instant now
    ) {
        return certificateRepository.findValidByScope(
            template,
            recipientMemberId,
            gisuId,
            meritTitle,
            CertificateStatus.ISSUED,
            now
        ).stream().findFirst();
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        return certificateRepository.existsBySerialNumber(serialNumber);
    }

    @Override
    public List<Certificate> listByRecipientMemberId(Long memberId) {
        return certificateRepository.findAllByRecipientMemberIdOrderByIssuedAtDescIdDesc(memberId);
    }

    @Override
    public Certificate save(Certificate certificate) {
        return certificateRepository.save(certificate);
    }
}
