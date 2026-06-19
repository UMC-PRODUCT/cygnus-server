package com.umc.product.notification.application.port.out;

import com.umc.product.notification.domain.FcmToken;
import java.util.List;
import java.util.Optional;

public interface LoadFcmPort {

    Optional<FcmToken> findByMemberIdAndToken(Long memberId, String fcmToken);

    List<FcmToken> listActiveByMemberId(Long memberId);

    List<FcmToken> listActiveByMemberIds(List<Long> memberIds);

    List<FcmToken> listActiveByToken(String fcmToken);

    List<FcmToken> listActiveByIds(List<Long> ids);

}
