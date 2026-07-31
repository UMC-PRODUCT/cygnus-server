package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LockRecruitingApplicantPort;

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
        if (gisuId == null || applicantMemberId == null) {
            throw new IllegalArgumentException("Recruiting applicant lock requires gisuId and memberId");
        }
        Stream<String> memberKey = Stream.of("recruiting:gisu:%d:member:%d".formatted(
            gisuId,
            applicantMemberId
        ));
        Stream<String> emailKeys = normalizedEmails.stream()
            .filter(email -> email != null && !email.isBlank())
            .map(email -> email.strip().toLowerCase(Locale.ROOT))
            .map(email -> "recruiting:gisu:%d:email:%s".formatted(gisuId, email));
        return Stream.concat(memberKey, emailKeys).distinct().sorted();
    }

    private void acquireTransactionLock(String lockKey) {
        entityManager.createNativeQuery("""
            SELECT pg_advisory_xact_lock(hashtextextended(CAST(:lockKey AS text), 0))
            """)
            .setParameter("lockKey", lockKey)
            .getSingleResult();
    }
}
