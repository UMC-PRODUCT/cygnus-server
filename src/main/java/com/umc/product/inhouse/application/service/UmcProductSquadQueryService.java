package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.inhouse.application.port.in.query.GetUmcProductSquadUseCase;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductSquadInfo;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductSquadQueryService implements GetUmcProductSquadUseCase {

    private final LoadUmcProductSquadPort loadUmcProductSquadPort;

    @Override
    public List<UmcProductSquadInfo> list(Boolean active, LocalDate activeOn) {
        return loadUmcProductSquadPort.listAll(active, activeOn).stream()
            .map(UmcProductSquadInfo::from)
            .toList();
    }
}
