package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductDepartment;

public interface SaveUmcProductDepartmentPort {

    UmcProductDepartment save(UmcProductDepartment department);

    void delete(UmcProductDepartment department);
}
