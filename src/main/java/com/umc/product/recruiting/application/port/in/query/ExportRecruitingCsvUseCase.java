package com.umc.product.recruiting.application.port.in.query;

public interface ExportRecruitingCsvUseCase {

    byte[] exportSummaryCsv(Long gisuId, Long schoolId, Long requesterMemberId);
}
