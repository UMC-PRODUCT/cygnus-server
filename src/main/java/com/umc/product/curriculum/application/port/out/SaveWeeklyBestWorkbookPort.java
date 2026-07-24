package com.umc.product.curriculum.application.port.out;

import com.umc.product.curriculum.domain.WeeklyBestWorkbook;

public interface SaveWeeklyBestWorkbookPort {

    WeeklyBestWorkbook save(WeeklyBestWorkbook weeklyBestWorkbook);

    void delete(WeeklyBestWorkbook weeklyBestWorkbook);
}
