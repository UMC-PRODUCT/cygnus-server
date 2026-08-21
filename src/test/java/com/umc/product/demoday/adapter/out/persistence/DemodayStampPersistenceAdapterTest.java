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
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayStampPort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    DemodayPollPersistenceAdapter.class,
    DemodayBoothPersistenceAdapter.class,
    DemodayEntryCodePersistenceAdapter.class,
    DemodayStampPersistenceAdapter.class
})
class DemodayStampPersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");
    private static final Long MEMBER_ID = 100L;

    @Autowired
    private LoadDemodayStampPort loadDemodayStampPort;

    @Autowired
    private SaveDemodayStampPort saveDemodayStampPort;

    @Autowired
    private SaveDemodayPollPort saveDemodayPollPort;

    @Autowired
    private SaveDemodayBoothPort saveDemodayBoothPort;

    @Autowired
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Autowired
    private SaveDemodayEntryCodePort saveDemodayEntryCodePort;

    @Autowired
    private LoadDemodayEntryCodePort loadDemodayEntryCodePort;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원 스탬프를 저장하고 식별자와 회원·부스 조건으로 조회한다")
    void saveAndFindMemberStamp() {
        // Given
        DemodayPoll poll = savePoll("회원 스탬프 조회");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayStamp savedStamp = saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, booth));

        entityManager.clear();

        // When & Then
        assertThat(loadDemodayStampPort.findById(savedStamp.getId()))
            .get()
            .extracting(DemodayStamp::getId)
            .isEqualTo(savedStamp.getId());
        assertThat(loadDemodayStampPort.findMemberStamp(MEMBER_ID, booth.getId()))
            .get()
            .extracting(DemodayStamp::getId)
            .isEqualTo(savedStamp.getId());
    }

    @Test
    @DisplayName("방문자 스탬프를 저장하고 입장 코드·부스 조건으로 조회한다")
    void saveAndFindVisitorStamp() {
        // Given
        DemodayPoll poll = savePoll("방문자 스탬프 조회");
        DemodayBooth booth = saveBooth(poll, 1L);
        DemodayEntryCode entryCode = saveEntryCode(poll, "a");
        DemodayStamp savedStamp = saveDemodayStampPort.save(DemodayStamp.forVisitor(entryCode, booth));

        entityManager.clear();

        // When & Then
        assertThat(loadDemodayStampPort.findVisitorStamp(entryCode.getId(), booth.getId()))
            .get()
            .extracting(DemodayStamp::getId)
            .isEqualTo(savedStamp.getId());
    }

    @Test
    @DisplayName("회원 스탬프 목록을 최근 생성된 순서로 조회한다")
    void listMemberStampsInRecentOrder() {
        // Given
        DemodayPoll poll = savePoll("회원 스탬프 목록");
        List<DemodayBooth> booths = saveDemodayBoothPort.saveAll(List.of(
            DemodayBooth.forProject(poll.getId(), 1, 1L),
            DemodayBooth.forProject(poll.getId(), 2, 2L)
        ));
        DemodayStamp firstStamp = saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, booths.get(0)));
        DemodayStamp secondStamp = saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, booths.get(1)));

        entityManager.clear();

        // When
        List<DemodayStamp> stamps = loadDemodayStampPort.listMemberStamps(MEMBER_ID);

        // Then
        assertThat(stamps)
            .extracting(DemodayStamp::getId)
            .containsExactly(secondStamp.getId(), firstStamp.getId());
    }

    @Test
    @DisplayName("방문자 스탬프 목록을 최근 생성된 순서로 조회한다")
    void listVisitorStampsInRecentOrder() {
        // Given
        DemodayPoll poll = savePoll("방문자 스탬프 목록");
        List<DemodayBooth> booths = saveDemodayBoothPort.saveAll(List.of(
            DemodayBooth.forProject(poll.getId(), 1, 1L),
            DemodayBooth.forProject(poll.getId(), 2, 2L)
        ));
        DemodayEntryCode entryCode = saveEntryCode(poll, "b");
        DemodayStamp firstStamp = saveDemodayStampPort.save(DemodayStamp.forVisitor(entryCode, booths.get(0)));
        DemodayStamp secondStamp = saveDemodayStampPort.save(DemodayStamp.forVisitor(entryCode, booths.get(1)));

        entityManager.clear();

        // When
        List<DemodayStamp> stamps = loadDemodayStampPort.listVisitorStamps(entryCode.getId());

        // Then
        assertThat(stamps)
            .extracting(DemodayStamp::getId)
            .containsExactly(secondStamp.getId(), firstStamp.getId());
    }

    @Test
    @DisplayName("무효화된 스탬프는 활성 스탬프 개수와 최근 활성 스탬프 조회에서 제외된다")
    void excludeRevokedStampFromActiveCountAndLatest() {
        // Given
        DemodayPoll poll = savePoll("무효화 스탬프 제외");
        List<DemodayBooth> booths = saveDemodayBoothPort.saveAll(List.of(
            DemodayBooth.forProject(poll.getId(), 1, 1L),
            DemodayBooth.forProject(poll.getId(), 2, 2L)
        ));
        DemodayStamp activeStamp = saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, booths.get(0)));
        DemodayStamp revokedStamp = saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, booths.get(1)));
        revokedStamp.revoke(Instant.parse("2026-08-19T10:00:00Z"));
        saveDemodayStampPort.save(revokedStamp);

        entityManager.clear();

        // When & Then
        assertThat(loadDemodayStampPort.countActiveMemberStamps(poll.getId(), MEMBER_ID)).isEqualTo(1);
        assertThat(loadDemodayStampPort.findLatestActiveMemberStamp(MEMBER_ID))
            .get()
            .extracting(DemodayStamp::getId)
            .isEqualTo(activeStamp.getId());
    }

    @Test
    @DisplayName("회원의 활성 스탬프 수는 Poll별로 독립적으로 계산한다")
    void countActiveMemberStampsWithinPoll() {
        // Given
        DemodayPoll firstPoll = savePoll("첫 번째 Poll 스탬프");
        DemodayPoll secondPoll = savePoll("두 번째 Poll 스탬프");
        DemodayBooth firstBooth = saveBooth(firstPoll, 1L);
        DemodayBooth secondBooth = saveBooth(secondPoll, 2L);
        saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, firstBooth));
        saveDemodayStampPort.save(DemodayStamp.forMember(MEMBER_ID, secondBooth));

        entityManager.clear();

        // When & Then
        assertThat(loadDemodayStampPort.countActiveMemberStamps(firstPoll.getId(), MEMBER_ID)).isEqualTo(1);
        assertThat(loadDemodayStampPort.countActiveMemberStamps(secondPoll.getId(), MEMBER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("영속화 후 다시 조회한 입장 코드와 부스의 투표가 다르면 스탬프를 생성할 수 없다")
    void rejectVisitorStampForBoothFromDifferentPollAfterReload() {
        // Given
        DemodayPoll firstPoll = savePoll("첫 번째 투표");
        DemodayPoll secondPoll = savePoll("두 번째 투표");
        DemodayEntryCode firstPollEntryCode = saveEntryCode(firstPoll, "c");
        DemodayBooth secondPollBooth = saveBooth(secondPoll, 2L);
        entityManager.flush();
        entityManager.clear();
        DemodayEntryCode reloadedEntryCode = loadDemodayEntryCodePort.findById(firstPollEntryCode.getId())
            .orElseThrow();
        DemodayBooth reloadedBooth = loadDemodayBoothPort.findById(secondPollBooth.getId()).orElseThrow();

        // When & Then
        assertThatThrownBy(() -> DemodayStamp.forVisitor(reloadedEntryCode, reloadedBooth))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH)
            );
    }

    private DemodayPoll savePoll(String name) {
        return saveDemodayPollPort.save(DemodayPoll.create(9L, name, OPENS_AT, CLOSES_AT));
    }

    private DemodayBooth saveBooth(DemodayPoll poll, Long projectId) {
        return saveDemodayBoothPort.save(
            DemodayBooth.forProject(poll.getId(), Math.toIntExact(projectId), projectId)
        );
    }

    private DemodayEntryCode saveEntryCode(DemodayPoll poll, String seed) {
        return saveDemodayEntryCodePort.save(
            DemodayEntryCode.create(poll.getId(), seed.repeat(DemodayEntryCode.HASH_LENGTH))
        );
    }
}
