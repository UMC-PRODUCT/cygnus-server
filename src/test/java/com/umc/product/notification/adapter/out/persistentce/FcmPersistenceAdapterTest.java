package com.umc.product.notification.adapter.out.persistentce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.umc.product.notification.domain.FcmToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmPersistenceAdapter")
class FcmPersistenceAdapterTest {

    @Mock
    FcmJpaRepository repository;

    @Test
    @DisplayName("조회 port의 모든 조건과 validation page 경계를 repository에 그대로 위임한다")
    void load_operations_delegate_to_repository() {
        FcmToken token = FcmToken.create(1L, "installation", "token");
        Instant validatedBefore = Instant.parse("2026-01-01T00:00:00Z");
        given(repository.findByInstallationId("installation")).willReturn(Optional.of(token));
        given(repository.findAllByMemberIdAndIsActiveTrue(1L)).willReturn(List.of(token));
        given(repository.findAllByMemberIdInAndIsActiveTrue(List.of(1L, 2L))).willReturn(List.of(token));
        given(repository.findAllByFcmTokenAndIsActiveTrue("token")).willReturn(List.of(token));
        given(repository.findAllByIdInAndIsActiveTrue(List.of(10L))).willReturn(List.of(token));
        given(repository.findActiveValidationTargets(validatedBefore, PageRequest.of(0, 25)))
            .willReturn(List.of(token));
        FcmPersistenceAdapter sut = new FcmPersistenceAdapter(repository);

        assertThat(sut.findByInstallationIdForUpdate("installation")).contains(token);
        assertThat(sut.listActiveByMemberId(1L)).containsExactly(token);
        assertThat(sut.listActiveByMemberIds(List.of(1L, 2L))).containsExactly(token);
        assertThat(sut.listActiveByToken("token")).containsExactly(token);
        assertThat(sut.listActiveByIds(List.of(10L))).containsExactly(token);
        assertThat(sut.listActiveForValidation(validatedBefore, 25)).containsExactly(token);
    }

    @Test
    @DisplayName("단건과 batch 저장을 repository에 위임한다")
    void save_operations_delegate_to_repository() {
        FcmToken first = FcmToken.create(1L, "installation-1", "token-1");
        FcmToken second = FcmToken.create(2L, "installation-2", "token-2");
        FcmPersistenceAdapter sut = new FcmPersistenceAdapter(repository);

        sut.save(first);
        sut.saveAll(List.of(first, second));

        then(repository).should().save(first);
        then(repository).should().saveAll(List.of(first, second));
    }
}
