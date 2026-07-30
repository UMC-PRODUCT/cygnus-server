package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;

public interface LoadRecruitingDecisionHistoryPort {

    Page<RecruitingDecisionHistoryRow> searchRows(RecruitingDecisionHistorySearchCondition condition, Pageable pageable);

    /**
     * 기수·학교 범위 내에서 판정을 수행한 담당자 member ID 목록을 중복 없이 반환합니다. 담당자 이름 검색에 사용합니다.
     */
    List<Long> listDeciderMemberIds(Long gisuId, Set<Long> schoolIds);
}
