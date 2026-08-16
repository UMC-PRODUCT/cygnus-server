package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeeklyBestWorkbookQueryRepository")
class WeeklyBestWorkbookQueryRepositoryTest {

    @Test
    @DisplayName("학교 필터의 회원 ID는 DB IN 절 제한을 피하도록 500개씩 분할한다")
    void memberIds_arePartitionedIntoBoundedChunks() {
        Set<Long> memberIds = LongStream.rangeClosed(1, 1_001)
            .boxed()
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Set<Long>> chunks = WeeklyBestWorkbookQueryRepository.partitionMemberIds(memberIds);

        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk).hasSizeLessThanOrEqualTo(500));
        assertThat(chunks.get(0)).containsExactlyElementsOf(LongStream.rangeClosed(1, 500).boxed().toList());
        assertThat(chunks.get(1)).containsExactlyElementsOf(LongStream.rangeClosed(501, 1_000).boxed().toList());
        assertThat(chunks.get(2)).containsExactly(1_001L);
        assertThat(chunks.stream().flatMap(Set::stream)).containsExactlyElementsOf(memberIds);
    }
}
