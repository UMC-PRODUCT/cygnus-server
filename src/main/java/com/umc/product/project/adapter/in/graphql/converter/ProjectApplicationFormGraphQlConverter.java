package com.umc.product.project.adapter.in.graphql.converter;

import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationFormGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationFormGraphQlResponse.ProjectApplicationFormOptionGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationFormGraphQlResponse.ProjectApplicationFormQuestionGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationFormGraphQlResponse.ProjectApplicationFormSectionGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectFormSectionType;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;

public final class ProjectApplicationFormGraphQlConverter {

    private ProjectApplicationFormGraphQlConverter() {
    }

    public static ProjectApplicationFormGraphQlResponse from(ApplicationFormInfo info) {
        return new ProjectApplicationFormGraphQlResponse(
            info.projectId(),
            info.applicationFormId(),
            info.title(),
            info.description(),
            info.sections().stream().map(ProjectApplicationFormGraphQlConverter::sectionFrom).toList()
        );
    }

    public static ProjectApplicationFormOptionGraphQlResponse optionFrom(ApplicationFormInfo.OptionInfo info) {
        return new ProjectApplicationFormOptionGraphQlResponse(
            info.optionId(),
            info.content(),
            info.orderNo(),
            info.isOther(),
            info.nextSectionId()
        );
    }

    private static ProjectApplicationFormSectionGraphQlResponse sectionFrom(ApplicationFormInfo.SectionInfo info) {
        return new ProjectApplicationFormSectionGraphQlResponse(
            info.sectionId(),
            ProjectFormSectionType.from(info.type()),
            info.allowedParts(),
            info.title(),
            info.description(),
            info.orderNo(),
            info.questions().stream().map(ProjectApplicationFormGraphQlConverter::questionFrom).toList()
        );
    }

    private static ProjectApplicationFormQuestionGraphQlResponse questionFrom(ApplicationFormInfo.QuestionInfo info) {
        return new ProjectApplicationFormQuestionGraphQlResponse(
            info.questionId(),
            info.type(),
            info.title(),
            info.description(),
            info.isRequired(),
            info.orderNo(),
            info.options().stream().map(ProjectApplicationFormGraphQlConverter::optionFrom).toList()
        );
    }
}
