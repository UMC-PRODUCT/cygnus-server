package com.umc.product.organization.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.organization.application.port.in.query.GetUmcProductChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductChapterQueryService implements GetUmcProductChapterUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;

    @Override
    public List<UmcProductChapterInfo> list(Boolean active) {
        return loadUmcProductChapterPort.listAll(active).stream()
            .map(UmcProductChapterInfo::from)
            .toList();
    }
}
