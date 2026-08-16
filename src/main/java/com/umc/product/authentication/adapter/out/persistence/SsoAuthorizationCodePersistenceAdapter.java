package com.umc.product.authentication.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authentication.application.port.out.LoadSsoAuthorizationCodePort;
import com.umc.product.authentication.application.port.out.SaveSsoAuthorizationCodePort;
import com.umc.product.authentication.domain.SsoAuthorizationCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SsoAuthorizationCodePersistenceAdapter implements LoadSsoAuthorizationCodePort, SaveSsoAuthorizationCodePort {

    private final SsoAuthorizationCodeJpaRepository repository;

    @Override
    public Optional<SsoAuthorizationCode> findByCodeHashForUpdate(String codeHash) {
        return repository.findByCodeHash(codeHash);
    }

    @Override
    public SsoAuthorizationCode save(SsoAuthorizationCode authorizationCode) {
        return repository.save(authorizationCode);
    }
}
