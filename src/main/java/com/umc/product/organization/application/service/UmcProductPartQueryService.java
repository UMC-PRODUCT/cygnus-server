package com.umc.product.organization.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.organization.application.port.in.query.GetUmcProductPartUseCase;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductPartQueryService implements GetUmcProductPartUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductPartPort loadUmcProductPartPort;

    @Override
    public List<UmcProductPartInfo> list(Long chapterId, Boolean active) {
        if (chapterId != null) {
            loadUmcProductChapterPort.getById(chapterId);
        }
        return loadUmcProductPartPort.listAll(chapterId, active).stream()
            .map(UmcProductPartInfo::from)
            .toList();
    }
}
