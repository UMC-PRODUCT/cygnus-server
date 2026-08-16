package com.umc.product.curriculum.application.port.out;

import com.umc.product.curriculum.domain.OriginalWorkbookMission;

public interface SaveOriginalWorkbookMissionPort {

    OriginalWorkbookMission save(OriginalWorkbookMission mission);

    void delete(OriginalWorkbookMission mission);
}
