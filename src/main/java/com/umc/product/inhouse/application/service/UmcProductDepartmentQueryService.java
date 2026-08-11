package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.inhouse.application.port.in.query.GetUmcProductDepartmentUseCase;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductDepartmentQueryService implements GetUmcProductDepartmentUseCase {

    private final LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;

    @Override
    public List<UmcProductDepartmentInfo> list(Boolean active, LocalDate activeOn) {
        return loadUmcProductDepartmentPort.listAll(active, activeOn).stream()
            .map(UmcProductDepartmentInfo::from)
            .toList();
    }
}
