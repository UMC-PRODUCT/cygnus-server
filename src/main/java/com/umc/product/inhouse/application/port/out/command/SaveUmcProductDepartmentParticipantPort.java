package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;

public interface SaveUmcProductDepartmentParticipantPort {

    UmcProductDepartmentParticipant save(UmcProductDepartmentParticipant participant);

    void delete(UmcProductDepartmentParticipant participant);

    void deleteAllByDepartmentId(Long departmentId);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
