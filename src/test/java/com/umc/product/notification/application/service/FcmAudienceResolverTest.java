package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmAudienceResolver")
class FcmAudienceResolverTest {

    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @InjectMocks
    FcmAudienceResolver sut;

    @Test
    @DisplayName("명시 회원과 target 회원을 순서 유지·중복 제거하고 누락 조직은 제외한다")
    void resolves_explicit_and_target_members_deterministically() {
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(List.of(
            challenger(1L, 1L, ChallengerPart.WEB),
            challenger(2L, 2L, ChallengerPart.SPRINGBOOT),
            challenger(3L, 3L, ChallengerPart.WEB)
        ));
        given(getMemberUseCase.findAllSchoolIdsByIds(Set.of(1L, 2L, 3L)))
            .willReturn(Map.of(1L, 10L, 2L, 11L));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of(10L, 11L)))
            .willReturn(Map.of(1L, Map.of(10L, new ChapterInfo(20L, "지부"))));

        assertThat(sut.resolve(event(List.of(99L, 1L, 99L), 1L, 20L, 10L, Set.of(ChallengerPart.WEB))))
            .containsExactly(99L, 1L);
    }

    @Test
    @DisplayName("target 기수 또는 challenger가 없으면 명시 회원만 반환한다")
    void missing_target_short_circuits() {
        assertThat(sut.resolve(event(List.of(1L), null, null, null, Set.of()))).containsExactly(1L);
        given(getChallengerUseCase.getAllByGisuId(1L)).willReturn(List.of());
        assertThat(sut.resolve(event(List.of(1L), 1L, null, null, Set.of()))).containsExactly(1L);
    }

    private FcmNotificationRequestedEvent event(
        List<Long> memberIds, Long gisuId, Long chapterId, Long schoolId, Set<ChallengerPart> parts
    ) {
        return new FcmNotificationRequestedEvent(
            UUID.randomUUID(), java.time.Instant.EPOCH, UUID.randomUUID(), 1L,
            memberIds, gisuId, chapterId, schoolId, parts, "제목", "본문", Map.of(), null, null
        );
    }

    private ChallengerInfo challenger(Long id, Long memberId, ChallengerPart part) {
        return ChallengerInfo.builder().challengerId(id).memberId(memberId).gisuId(1L).part(part).build();
    }
}
