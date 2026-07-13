package com.umc.product.organization.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.organization.application.port.in.query.GetUmcProductOrganizationChartUseCase;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductPart;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductOrganizationChartQueryService implements GetUmcProductOrganizationChartUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductPartPort loadUmcProductPartPort;
    private final LoadUmcProductSquadPort loadUmcProductSquadPort;
    private final UmcProductDateProvider umcProductDateProvider;

    @Override
    public UmcProductOrganizationChartInfo getCurrent() {
        LocalDate today = umcProductDateProvider.today();
        List<UmcProductChapter> chapters = loadUmcProductChapterPort.listAll(true);
        List<Long> chapterIds = chapters.stream()
            .map(UmcProductChapter::getId)
            .toList();
        Map<Long, List<UmcProductPart>> partsByChapterId = loadUmcProductPartPort
            .listByChapterIds(chapterIds, true)
            .stream()
            .collect(Collectors.groupingBy(part -> part.getChapter().getId()));

        List<UmcProductOrganizationChartChapterInfo> chapterInfos = chapters.stream()
            .map(chapter -> toChapterInfo(chapter, partsByChapterId.getOrDefault(chapter.getId(), List.of())))
            .toList();
        List<UmcProductSquadInfo> squadInfos = loadUmcProductSquadPort.listAll(true, today).stream()
            .map(UmcProductSquadInfo::from)
            .toList();
        return new UmcProductOrganizationChartInfo(chapterInfos, squadInfos);
    }

    private UmcProductOrganizationChartChapterInfo toChapterInfo(
        UmcProductChapter chapter,
        List<UmcProductPart> parts
    ) {
        UmcProductChapterInfo chapterInfo = UmcProductChapterInfo.from(chapter);
        return new UmcProductOrganizationChartChapterInfo(
            chapterInfo,
            parts.stream()
                .map(part -> UmcProductPartInfo.from(part, chapterInfo))
                .toList()
        );
    }
}
