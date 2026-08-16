package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LockRecruitingApplicantPort;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class RecruitingApplicantLockPersistenceAdapter implements LockRecruitingApplicantPort {

    private static final String LOCK_TIMEOUT = "3s";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void lockByGisuAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        Collection<String> normalizedEmails
    ) {
        RecruitingLockExceptionTranslator.translate(() -> {
            entityManager.createNativeQuery("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT + "'")
                .executeUpdate();
            lockKeys(gisuId, applicantMemberId, normalizedEmails)
                .forEach(this::acquireTransactionLock);
            return null;
        });
    }

    private Stream<String> lockKeys(
        Long gisuId,
        Long applicantMemberId,
        Collection<String> normalizedEmails
    ) {
        if (gisuId == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICANT_IDENTITY_REQUIRED);
        }
        Stream<String> memberKey = applicantMemberId == null
            ? Stream.empty()
            : Stream.of("recruiting:gisu:%d:member:%d".formatted(gisuId, applicantMemberId));
        Stream<String> emailKeys = normalizedEmails.stream()
            .filter(email -> email != null && !email.isBlank())
            .map(email -> email.strip().toLowerCase(Locale.ROOT))
            .map(email -> "recruiting:gisu:%d:email:%s".formatted(gisuId, email));
        List<String> keys = Stream.concat(memberKey, emailKeys).distinct().sorted().toList();
        if (keys.isEmpty()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICANT_IDENTITY_REQUIRED);
        }
        return keys.stream();
    }

    private void acquireTransactionLock(String lockKey) {
        entityManager.createNativeQuery("""
            SELECT pg_advisory_xact_lock(hashtextextended(CAST(:lockKey AS text), 0))
            """)
            .setParameter("lockKey", lockKey)
            .getSingleResult();
    }
}
