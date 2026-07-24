package com.umc.product.curriculum.application.port.out;

import com.umc.product.curriculum.domain.ChallengerWorkbook;

public interface SaveChallengerWorkbookPort {

    ChallengerWorkbook save(ChallengerWorkbook challengerWorkbook);

    void delete(ChallengerWorkbook challengerWorkbook);
}
