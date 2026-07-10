package com.umc.product.notification.adapter.out.persistentce;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.domain.FcmToken;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FcmPersistenceAdapter implements LoadFcmPort, SaveFcmPort {

    private final FcmJpaRepository fcmJpaRepository;

    @Override
    public Optional<FcmToken> findByMemberIdAndToken(Long memberId, String fcmToken) {
        return fcmJpaRepository.findByMemberIdAndFcmToken(memberId, fcmToken);
    }

    @Override
    public List<FcmToken> listActiveByMemberId(Long memberId) {
        return fcmJpaRepository.findAllByMemberIdAndIsActiveTrue(memberId);
    }

    @Override
    public List<FcmToken> listActiveByMemberIds(List<Long> memberIds) {
        return fcmJpaRepository.findAllByMemberIdInAndIsActiveTrue(memberIds);
    }

    @Override
    public List<FcmToken> listActiveByToken(String fcmToken) {
        return fcmJpaRepository.findAllByFcmTokenAndIsActiveTrue(fcmToken);
    }

    @Override
    public List<FcmToken> listActiveByIds(List<Long> ids) {
        return fcmJpaRepository.findAllByIdInAndIsActiveTrue(ids);
    }

    @Override
    public List<FcmToken> listActiveForValidation(Instant validatedBefore, int limit) {
        return fcmJpaRepository.findActiveValidationTargets(validatedBefore, PageRequest.of(0, limit));
    }

    @Override
    public void save(FcmToken fcmToken) {
        fcmJpaRepository.save(fcmToken);
    }

    @Override
    public void saveAll(List<FcmToken> fcmTokens) {
        fcmJpaRepository.saveAll(fcmTokens);
    }
}
