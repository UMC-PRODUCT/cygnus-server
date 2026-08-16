package com.umc.product.test.application.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationFormSectionEntry;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationQuestionEntry;
import com.umc.product.project.domain.enums.FormSectionType;

/**
 * 시나리오 시딩용 최소 ApplicationForm 스펙 생성기.
 * <p>
 * COMMON 섹션 1 개 + LONG_TEXT 질문 1 개의 가장 단순한 구조를 만든다. 옵션이 필요한 질문 타입
 * (RADIO/CHECKBOX/DROPDOWN) 은 시딩 본문에서 제외해 도메인 검증 ({@code APPLICATION_FORM_OPTIONS_REQUIRED}) 통과를 보장한다.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DummyApplicationFormFactory {

    public List<ApplicationFormSectionEntry> defaultSections() {
        return List.of(
            ApplicationFormSectionEntry.builder()
                .sectionId(null)
                .type(FormSectionType.COMMON)
                .allowedParts(null)
                .title("공통 질문")
                .description("시딩 더미 공통 섹션")
                .orderNo(0L)
                .questions(List.of(
                    ApplicationQuestionEntry.builder()
                        .questionId(null)
                        .type(QuestionType.LONG_TEXT)
                        .title("지원 동기를 알려주세요")
                        .description(null)
                        .isRequired(true)
                        .orderNo(0L)
                        .options(List.of())
                        .build()
                ))
                .build()
        );
    }
}
