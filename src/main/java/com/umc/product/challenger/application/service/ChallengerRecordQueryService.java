package com.umc.product.challenger.application.service;

import com.umc.product.challenger.application.port.in.query.GetChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerRecordInfo;
import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengerRecordQueryService implements GetChallengerRecordUseCase {

    private final LoadChallengerRecordPort loadChallengerRecordPort;

    @Override
    public ChallengerRecordInfo getById(Long id) {
        return ChallengerRecordInfo.from(loadChallengerRecordPort.getById(id));
    }

    @Override
    public ChallengerRecordInfo getByCode(String code) {
        return ChallengerRecordInfo.from(loadChallengerRecordPort.getByCode(code));
    }

    @Override
    public List<ChallengerRecordInfo> getBySchoolId(Long schoolId) {
        return loadChallengerRecordPort.findBySchoolId(schoolId).stream()
            .map(ChallengerRecordInfo::from)
            .toList();
    }

    @Override
    public List<ChallengerRecordInfo> getByChapterId(Long chapterId) {
        return loadChallengerRecordPort.findByChapterId(chapterId).stream()
            .map(ChallengerRecordInfo::from)
            .toList();
    }

    @Override
    public Page<ChallengerRecordInfo> search(ListChallengerRecordsQuery query) {
        return loadChallengerRecordPort.search(query)
            .map(ChallengerRecordInfo::from);
    }
}
