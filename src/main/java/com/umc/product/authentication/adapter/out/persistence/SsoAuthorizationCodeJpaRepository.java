package com.umc.product.authentication.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.umc.product.authentication.domain.SsoAuthorizationCode;

import jakarta.persistence.LockModeType;

public interface SsoAuthorizationCodeJpaRepository extends JpaRepository<SsoAuthorizationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SsoAuthorizationCode> findByCodeHash(String codeHash);
}
