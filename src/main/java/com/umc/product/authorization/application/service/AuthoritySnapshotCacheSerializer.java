package com.umc.product.authorization.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.application.service.dto.AuthoritySnapshotCacheDto;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthoritySnapshotCacheSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(AuthoritySnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(AuthoritySnapshotCacheDto.from(snapshot));
        } catch (JsonProcessingException e) {
            throw new AuthorizationDomainException(
                AuthorizationErrorCode.POLICY_EVALUATION_FAILED,
                "권한 snapshot 캐시 직렬화에 실패했습니다."
            );
        }
    }

    public AuthoritySnapshot deserialize(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new AuthorizationDomainException(
                AuthorizationErrorCode.POLICY_EVALUATION_FAILED,
                "권한 snapshot 캐시 값이 비어 있습니다."
            );
        }

        try {
            return objectMapper.readValue(payload, AuthoritySnapshotCacheDto.class).toDomain();
        } catch (JsonProcessingException e) {
            throw new AuthorizationDomainException(
                AuthorizationErrorCode.POLICY_EVALUATION_FAILED,
                "권한 snapshot 캐시 역직렬화에 실패했습니다."
            );
        }
    }
}
