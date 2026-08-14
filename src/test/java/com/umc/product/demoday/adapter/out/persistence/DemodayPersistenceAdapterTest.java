package com.umc.product.demoday.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayStampPort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    DemodayPollPersistenceAdapter.class,
    DemodayBoothPersistenceAdapter.class,
    DemodayEntryCodePersistenceAdapter.class,
    DemodayVotePersistenceAdapter.class,
    DemodayStampPersistenceAdapter.class
})
class DemodayPersistenceAdapterTest {

    private static final Instant OPENS_AT = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-07-27T12:00:00Z");
    private static final Long MEMBER_ID = 100L;

    @Autowired
    private LoadDemodayPollPort loadDemodayPollPort;

    @Autowired
    private SaveDemodayPollPort saveDemodayPollPort;

    @Autowired
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Autowired
    private SaveDemodayBoothPort saveDemodayBoothPort;

    @Autowired
    private LoadDemodayEntryCodePort loadDemodayEntryCodePort;

    @Autowired
    private SaveDemodayEntryCodePort saveDemodayEntryCodePort;

    @Autowired
    private LoadDemodayVotePort loadDemodayVotePort;

    @Autowired
    private SaveDemodayVotePort saveDemodayVotePort;

    @Autowired
    private LoadDemodayStampPort loadDemodayStampPort;

    @Autowired
    private SaveDemodayStampPort saveDemodayStampPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("데모데이 투표 도메인 객체를 저장한 뒤 모든 조회 포트로 다시 조회한다")
    void saveAndLoadDemodayVoteDomain() {
        // Given
        DemodayPoll poll = saveDemodayPollPort.save(
            DemodayPoll.create(9L, "9기 데모데이", OPENS_AT, CLOSES_AT)
        );
        List<DemodayBooth> booths = saveDemodayBoothPort.saveAll(List.of(
            DemodayBooth.forProject(poll.getId(), 10L),
            DemodayBooth.forExternal(poll.getId(), "외부 참가팀")
        ));
        DemodayBooth projectBooth = booths.get(0);
        DemodayBooth externalBooth = booths.get(1);
        DemodayEntryCode entryCode = saveDemodayEntryCodePort.save(
            DemodayEntryCode.create(poll.getId(), "a".repeat(DemodayEntryCode.HASH_LENGTH))
        );

        DemodayVote memberVote = saveDemodayVotePort.save(
            DemodayVote.forMember(poll.getId(), MEMBER_ID, projectBooth)
        );
        DemodayVote visitorVote = saveDemodayVotePort.save(
            DemodayVote.forVisitor(poll.getId(), entryCode, externalBooth)
        );
        DemodayStamp memberStamp = saveDemodayStampPort.save(
            DemodayStamp.forMember(MEMBER_ID, projectBooth)
        );
        DemodayStamp visitorStamp = saveDemodayStampPort.save(
            DemodayStamp.forVisitor(entryCode, externalBooth)
        );

        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertThat(loadDemodayPollPort.findById(poll.getId())).isPresent();
        assertThat(loadDemodayPollPort.listAll())
            .extracting(DemodayPoll::getId)
            .contains(poll.getId());

        assertThat(loadDemodayBoothPort.findById(projectBooth.getId())).isPresent();
        assertThat(loadDemodayBoothPort.listByPollId(poll.getId()))
            .extracting(DemodayBooth::getId)
            .containsExactly(projectBooth.getId(), externalBooth.getId());

        assertThat(loadDemodayEntryCodePort.findById(entryCode.getId())).isPresent();
        assertThat(loadDemodayEntryCodePort.findByCodeHash(entryCode.getCodeHash()))
            .get()
            .extracting(DemodayEntryCode::getId)
            .isEqualTo(entryCode.getId());

        assertThat(loadDemodayVotePort.findById(memberVote.getId())).isPresent();
        assertThat(loadDemodayVotePort.findByPollIdAndMemberId(poll.getId(), MEMBER_ID))
            .get()
            .extracting(DemodayVote::getId)
            .isEqualTo(memberVote.getId());
        assertThat(loadDemodayVotePort.findByEntryCodeId(entryCode.getId()))
            .get()
            .extracting(DemodayVote::getId)
            .isEqualTo(visitorVote.getId());
        assertThat(loadDemodayVotePort.listByPollId(poll.getId()))
            .extracting(DemodayVote::getId)
            .containsExactly(visitorVote.getId(), memberVote.getId());

        assertThat(loadDemodayStampPort.findById(memberStamp.getId())).isPresent();
        assertThat(loadDemodayStampPort.findByMemberIdAndBoothId(MEMBER_ID, projectBooth.getId()))
            .get()
            .extracting(DemodayStamp::getId)
            .isEqualTo(memberStamp.getId());
        assertThat(loadDemodayStampPort.findByEntryCodeIdAndBoothId(entryCode.getId(), externalBooth.getId()))
            .get()
            .extracting(DemodayStamp::getId)
            .isEqualTo(visitorStamp.getId());
        assertThat(loadDemodayStampPort.listByMemberId(MEMBER_ID))
            .extracting(DemodayStamp::getId)
            .containsExactly(memberStamp.getId());
        assertThat(loadDemodayStampPort.listByEntryCodeId(entryCode.getId()))
            .extracting(DemodayStamp::getId)
            .containsExactly(visitorStamp.getId());
    }
}
