package com.umc.product.organization.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolLinkInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.UnassignedSchoolInfo;


public interface GetSchoolUseCase {

    List<SchoolNameInfo> getAllSchoolNames();

    SchoolDetailInfo getSchoolDetail(Long schoolId);

    SchoolLinkInfo getSchoolLink(Long schoolId);

    List<SchoolDetailInfo> listDetailsByIds(Set<Long> schoolIds);

    List<UnassignedSchoolInfo> getUnassignedSchools(Long gisuId);

    List<SchoolDetailInfo> getSchoolListByGisuId(Long gisuId);

    Map<Long, List<SchoolDetailInfo>> getSchoolListByGisuIds(Set<Long> gisuIds);

    List<SchoolNameInfo> batchGetNamesByIds(Set<Long> schoolIds);
}
