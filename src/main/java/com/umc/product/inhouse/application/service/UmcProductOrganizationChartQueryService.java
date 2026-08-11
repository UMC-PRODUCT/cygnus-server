package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.inhouse.application.port.in.query.GetUmcProductOrganizationChartUseCase;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductOrganizationChartInfo;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductOrganizationChartQueryService implements GetUmcProductOrganizationChartUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;
    private final UmcProductDateProvider umcProductDateProvider;

    @Override
    public UmcProductOrganizationChartInfo getCurrent() {
        LocalDate today = umcProductDateProvider.today();
        List<UmcProductChapterInfo> chapterInfos = loadUmcProductChapterPort.listAll(true).stream()
            .map(UmcProductChapterInfo::from)
            .toList();
        List<UmcProductDepartmentInfo> departmentInfos = loadUmcProductDepartmentPort.listAll(true, today).stream()
            .map(UmcProductDepartmentInfo::from)
            .toList();
        return new UmcProductOrganizationChartInfo(chapterInfos, departmentInfos);
    }

}
