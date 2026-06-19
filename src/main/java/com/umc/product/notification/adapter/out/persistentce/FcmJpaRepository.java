package com.umc.product.notification.adapter.out.persistentce;

import com.umc.product.notification.domain.FcmToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmJpaRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByMemberIdAndFcmToken(Long memberId, String fcmToken);

    List<FcmToken> findAllByMemberIdAndIsActiveTrue(Long memberId);

    List<FcmToken> findAllByMemberIdInAndIsActiveTrue(List<Long> memberIds);

    List<FcmToken> findAllByFcmTokenAndIsActiveTrue(String fcmToken);

    List<FcmToken> findAllByIdInAndIsActiveTrue(List<Long> ids);

}
