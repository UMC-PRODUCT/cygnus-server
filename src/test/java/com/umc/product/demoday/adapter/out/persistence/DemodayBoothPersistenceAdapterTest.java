package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    DemodayPollPersistenceAdapter.class,
    DemodayBoothPersistenceAdapter.class
})
class DemodayBoothPersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");

    @Autowired
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Autowired
    private SaveDemodayBoothPort saveDemodayBoothPort;

    @Autowired
    private SaveDemodayPollPort saveDemodayPollPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("데모데이 부스를 저장한 뒤 식별자로 조회한다")
    void saveAndFindBoothById() {
        // Given
        DemodayPoll poll = savePoll();
        DemodayBooth booth = saveDemodayBoothPort.save(DemodayBooth.forProject(poll.getId(), 11, 1L));
        clearPersistenceContext();

        // When
        DemodayBooth found = loadDemodayBoothPort.findById(booth.getId()).orElseThrow();

        // Then
        assertThat(found.getId()).isEqualTo(booth.getId());
        assertThat(found.getBoothCode()).isEqualTo(11);
    }

    @Test
    @DisplayName("부스 목록을 입력 순서와 대응하는 결과 목록으로 일괄 저장한다")
    void saveAllBoothsInInputOrder() {
        // Given
        DemodayPoll poll = savePoll();
        List<DemodayBooth> booths = List.of(
            DemodayBooth.forProject(poll.getId(), 11, 1L),
            DemodayBooth.forExternal(poll.getId(), 12, "외부 참가팀")
        );

        // When
        List<DemodayBooth> saved = saveDemodayBoothPort.saveAll(booths);

        // Then
        assertThat(saved)
            .extracting(DemodayBooth::getProjectId)
            .containsExactly(1L, null);
    }

    @Test
    @DisplayName("투표의 부스 목록을 부스 코드 오름차순으로 조회한다")
    void listBoothsByCodeAscending() {
        // Given
        DemodayPoll poll = savePoll();
        saveDemodayBoothPort.saveAll(List.of(
            DemodayBooth.forProject(poll.getId(), 20, 1L),
            DemodayBooth.forProject(poll.getId(), 10, 2L)
        ));
        clearPersistenceContext();

        // When
        List<DemodayBooth> found = loadDemodayBoothPort.listByPollId(poll.getId());

        // Then
        assertThat(found)
            .extracting(DemodayBooth::getBoothCode)
            .containsExactly(10, 20);
    }

    @Test
    @DisplayName("같은 투표에 같은 부스 코드를 저장하면 중복 코드 도메인 오류로 변환한다")
    void rejectDuplicateBoothCodeInSamePoll() {
        // Given
        DemodayPoll poll = savePoll();
        saveDemodayBoothPort.save(DemodayBooth.forProject(poll.getId(), 11, 1L));

        // When & Then
        assertThatThrownBy(() -> saveDemodayBoothPort.save(
            DemodayBooth.forExternal(poll.getId(), 11, "외부 참가팀")
        )).isInstanceOfSatisfying(DemodayDomainException.class, exception ->
            assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_CODE_DUPLICATED)
        );
    }

    @Test
    @DisplayName("일괄 저장 안의 부스 코드 중복도 도메인 오류로 변환한다")
    void rejectDuplicateBoothCodeInBatch() {
        // Given
        DemodayPoll poll = savePoll();
        List<DemodayBooth> booths = List.of(
            DemodayBooth.forProject(poll.getId(), 11, 1L),
            DemodayBooth.forExternal(poll.getId(), 11, "외부 참가팀")
        );

        // When & Then
        assertThatThrownBy(() -> saveDemodayBoothPort.saveAll(booths))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_CODE_DUPLICATED)
            );
    }

    @Test
    @DisplayName("스탬프 credential 해시로 부스를 조회한다")
    void findBoothByStampCredentialHash() {
        // Given
        DemodayPoll poll = savePoll();
        DemodayBooth booth = DemodayBooth.forProject(poll.getId(), 11, 1L);
        booth.applyStampCredential("credential-hash", "encrypted-credential", Instant.parse("2026-08-19T00:00:00Z"));
        DemodayBooth saved = saveDemodayBoothPort.save(booth);
        clearPersistenceContext();

        // When & Then
        assertThat(loadDemodayBoothPort.findByStampCredentialHash("credential-hash"))
            .get()
            .extracting(DemodayBooth::getId)
            .isEqualTo(saved.getId());
        assertThat(loadDemodayBoothPort.findByStampCredentialHash("no-such-hash"))
            .isEmpty();
    }

    private DemodayPoll savePoll() {
        return saveDemodayPollPort.save(DemodayPoll.create(9L, "9기 데모데이", OPENS_AT, CLOSES_AT));
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
