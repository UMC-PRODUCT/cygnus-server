package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Set;

import com.umc.product.demoday.domain.DemodayVote;

public interface SearchDemodayVotePort {

    List<DemodayVote> search(DemodayVoteSearchCondition condition);

    Set<Long> listMemberIds(Long pollId, Long boothId);
}
