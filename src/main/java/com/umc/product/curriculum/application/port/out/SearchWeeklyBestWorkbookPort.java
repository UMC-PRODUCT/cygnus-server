package com.umc.product.curriculum.application.port.out;

import org.springframework.data.domain.Page;

import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;

public interface SearchWeeklyBestWorkbookPort {

    Page<WeeklyBestWorkbook> searchBestWorkbooks(GetBestWorkbooksQuery query);
}
