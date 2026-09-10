package com.umc.product.curriculum.adapter.out.persistence;

import static com.umc.product.curriculum.domain.QCurriculum.curriculum;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.global.exception.NotImplementedException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CurriculumQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<CurriculumProjection> findByGisuIdAndPart(Long gisuId, ChallengerPart part) {
        return Optional.ofNullable(
            queryFactory
                .select(Projections.constructor(CurriculumProjection.class,
                    curriculum.id,
                    curriculum.part,
                    curriculum.track,
                    curriculum.title
                ))
                .from(curriculum)
                .where(curriculum.gisuId.eq(gisuId), curriculum.part.eq(part))
                .fetchOne()
        );
    }

    public Optional<CurriculumProjection> findByGisuIdAndTrack(Long gisuId, ChallengerTrack track) {
        return Optional.ofNullable(queryFactory
            .select(Projections.constructor(CurriculumProjection.class,
                curriculum.id, curriculum.part, curriculum.track, curriculum.title))
            .from(curriculum)
            .where(curriculum.gisuId.eq(gisuId), curriculum.track.eq(track))
            .fetchOne());
    }

    /**
     * 미배포 상태이면서 시작일이 지난 워크북 목록 조회 (자동 배포 대상)
     */
    public List<OriginalWorkbook> findUnreleasedWorkbookIdsWithStartDateBefore(Instant now) {
        throw new NotImplementedException();
        //        return queryFactory
//            .selectFrom(originalWorkbook)
//            .join(originalWorkbook.curriculum, curriculum).fetchJoin()
//            .where(
//                originalWorkbook.releasedAt.isNull(),
//                originalWorkbook.startDate.before(now)
//            )
//            .fetch();
    }
}
