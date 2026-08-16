package com.umc.product.organization.application.port.out.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolGisuChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolListItemInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolSearchCondition;
import com.umc.product.organization.domain.School;

public interface LoadSchoolPort {

    Page<SchoolListItemInfo> findSchools(SchoolSearchCondition condition, Pageable pageable);

    List<SchoolNameInfo> findAllNames();

    School findSchoolDetailById(Long schoolId);

    School findById(Long schoolId);

    SchoolChapterInfo findSchoolDetailByIdWithActiveChapter(Long schoolId);

    List<SchoolDetailInfo.SchoolLinkItem> findLinksBySchoolId(Long schoolId);

    Map<Long, List<SchoolDetailInfo.SchoolLinkItem>> findLinksBySchoolIds(List<Long> schoolIds);

    List<SchoolChapterInfo> findSchoolDetailsByIds(Set<Long> schoolIds);

    List<SchoolChapterInfo> findSchoolDetailsByGisuId(Long gisuId);

    List<SchoolChapterNameInfo> findSchoolChapterNamesByGisuId(Long gisuId);

    List<SchoolGisuChapterInfo> findSchoolDetailsByGisuIds(Set<Long> gisuIds);

    List<School> findAllByIds(List<Long> schoolIds);

    List<School> findUnassignedByGisuId(Long gisuId);

    boolean existsById(Long schoolId);

    void throwIfNotExists(Long schoolId);
}
