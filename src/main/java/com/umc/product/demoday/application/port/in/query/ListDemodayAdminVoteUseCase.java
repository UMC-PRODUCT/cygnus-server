package com.umc.product.demoday.application.port.in.query;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteListInfo;
import com.umc.product.demoday.application.port.in.query.dto.ListDemodayAdminVoteQuery;

public interface ListDemodayAdminVoteUseCase {

    DemodayAdminVoteListInfo listVotes(ListDemodayAdminVoteQuery query);
}
