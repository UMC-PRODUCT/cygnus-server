package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.inhouse.application.port.in.query.GetUmcProductOrganizationChartUseCase;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductOrganizationChartInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductSquadInfo;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductOrganizationChartQueryService implements GetUmcProductOrganizationChartUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductSquadPort loadUmcProductSquadPort;
    private final UmcProductDateProvider umcProductDateProvider;

    @Override
    public UmcProductOrganizationChartInfo getCurrent() {
        LocalDate today = umcProductDateProvider.today();
        List<UmcProductChapterInfo> chapterInfos = loadUmcProductChapterPort.listAll(true).stream()
            .map(UmcProductChapterInfo::from)
            .toList();
        List<UmcProductSquadInfo> squadInfos = loadUmcProductSquadPort.listAll(true, today).stream()
            .map(UmcProductSquadInfo::from)
            .toList();
        return new UmcProductOrganizationChartInfo(chapterInfos, squadInfos);
    }

}
