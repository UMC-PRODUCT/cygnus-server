package com.umc.product.challenger.application.port.in.query;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerRecordInfo;
import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;

public interface GetChallengerRecordUseCase {
    ChallengerRecordInfo getById(Long id);

    ChallengerRecordInfo getByCode(String code);

    List<ChallengerRecordInfo> getBySchoolId(Long schoolId);

    List<ChallengerRecordInfo> getByChapterId(Long chapterId);

    Page<ChallengerRecordInfo> search(ListChallengerRecordsQuery query);
}
