package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    DemodayPollPersistenceAdapter.class,
    DemodayEntryCodePersistenceAdapter.class
})
class DemodayEntryCodePersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");

    @Autowired
    private LoadDemodayEntryCodePort loadDemodayEntryCodePort;

    @Autowired
    private SaveDemodayEntryCodePort saveDemodayEntryCodePort;

    @Autowired
    private SaveDemodayPollPort saveDemodayPollPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("입장 코드를 저장한 뒤 식별자와 코드 해시로 조회한다")
    void saveAndFindEntryCodeByIdAndCodeHash() {
        // Given
        DemodayPoll poll = savePoll();
        DemodayEntryCode entryCode = saveDemodayEntryCodePort.save(newEntryCode(poll, "a"));
        clearPersistenceContext();

        // When
        DemodayEntryCode foundById = loadDemodayEntryCodePort.findById(entryCode.getId()).orElseThrow();
        DemodayEntryCode foundByHash = loadDemodayEntryCodePort.findByCodeHash(entryCode.getCodeHash()).orElseThrow();
        DemodayEntryCode foundForRedemption = loadDemodayEntryCodePort
            .findByCodeHashForRedemption(entryCode.getCodeHash())
            .orElseThrow();

        // Then
        assertThat(foundById.getId()).isEqualTo(entryCode.getId());
        assertThat(foundByHash.getId()).isEqualTo(entryCode.getId());
        assertThat(foundForRedemption.getId()).isEqualTo(entryCode.getId());
    }

    @Test
    @DisplayName("입장 코드 목록을 입력 순서와 대응하는 결과 목록으로 일괄 저장한다")
    void saveAllEntryCodesInInputOrder() {
        // Given
        DemodayPoll poll = savePoll();
        List<DemodayEntryCode> entryCodes = List.of(
            newEntryCode(poll, "a"),
            newEntryCode(poll, "b")
        );

        // When
        List<DemodayEntryCode> saved = saveDemodayEntryCodePort.saveAll(entryCodes);

        // Then
        assertThat(saved)
            .extracting(DemodayEntryCode::getCodeHash)
            .containsExactly(entryCodes.get(0).getCodeHash(), entryCodes.get(1).getCodeHash());
    }

    @Test
    @DisplayName("입장 코드 사용 요청 식별자는 해시로 저장하고 다시 조회할 수 있다")
    void saveAndFindRedemptionRequestIdHash() {
        // Given
        DemodayPoll poll = savePoll();
        DemodayEntryCode entryCode = newEntryCode(poll, "c");
        String requestIdHash = "d".repeat(DemodayEntryCode.HASH_LENGTH);
        entryCode.redeemOrResume(Instant.parse("2026-07-27T10:00:00Z"), requestIdHash);
        DemodayEntryCode saved = saveDemodayEntryCodePort.save(entryCode);
        clearPersistenceContext();

        // When
        DemodayEntryCode found = loadDemodayEntryCodePort.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getRedemptionRequestIdHash()).isEqualTo(requestIdHash);
    }

    private DemodayPoll savePoll() {
        return saveDemodayPollPort.save(DemodayPoll.create(9L, "9기 데모데이", OPENS_AT, CLOSES_AT));
    }

    private DemodayEntryCode newEntryCode(DemodayPoll poll, String seed) {
        return DemodayEntryCode.create(poll.getId(), seed.repeat(DemodayEntryCode.HASH_LENGTH));
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
