package com.umc.product.form.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FormSectionCommandService implements ManageFormSectionUseCase {

    private final LoadFormPort loadFormPort;
    private final LoadFormSectionPort loadFormSectionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final SaveFormSectionPort saveFormSectionPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;

    @Override
    public Long createSection(CreateFormSectionCommand command) {
        Form form = loadFormPort.findById(command.formId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        long nextOrderNo = loadFormSectionPort.listByFormId(command.formId()).stream()
            .mapToLong(FormSection::getOrderNo)
            .max()
            .orElse(0L) + 1L;

        FormSection section = FormSection.create(form, command.title(), command.description(), nextOrderNo);
        return saveFormSectionPort.save(section).getId();
    }

    @Override
    public void updateSection(UpdateFormSectionCommand command) {
        FormSection section = loadFormSectionPort.findById(command.sectionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        section.update(command.title(), command.description(), Boolean.TRUE.equals(command.clearDescription()));
        saveFormSectionPort.save(section);
    }

    @Override
    public void deleteSection(DeleteFormSectionCommand command) {
        Long sectionId = command.sectionId();

        // cascade (자식부터)
        saveQuestionOptionPort.deleteBySectionId(sectionId);
        saveQuestionPort.deleteBySectionId(sectionId);
        saveFormSectionPort.deleteById(sectionId);
    }

    @Override
    public void reorderSections(ReorderFormSectionsCommand command) {
        List<FormSection> sections = loadFormSectionPort.listByFormId(command.formId());

        Set<Long> existingIds = sections.stream()
            .map(FormSection::getId)
            .collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(command.orderedSectionIds());

        if (!existingIds.equals(requestedIds)) {
            throw new FormDomainException(
                FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "재배치 요청의 섹션 ID 셋이 실제 폼의 섹션 ID 셋과 일치하지 않습니다."
            );
        }

        Map<Long, FormSection> byId = sections.stream()
            .collect(Collectors.toMap(FormSection::getId, Function.identity()));

        for (int i = 0; i < command.orderedSectionIds().size(); i++) {
            byId.get(command.orderedSectionIds().get(i)).updateOrderNo((long) (i + 1));
        }

        validateForwardOnlyAfterReorder(command.formId(), byId);

        saveFormSectionPort.saveAll(sections);
    }

    /**
     * 재배치로 인해 기존 옵션의 {@code nextSectionId} 가 back-edge 로 바뀌지 않는지 검증한다.
     * <p>
     * 폼의 모든 질문/옵션을 순회해 옵션이 가리키는 대상 섹션의 새 orderNo 가 옵션 자신의 섹션 새 orderNo 보다 큰지 확인한다.
     * 위반 옵션이 하나라도 있으면 재배치 자체를 거부한다.
     */
    private void validateForwardOnlyAfterReorder(Long formId, Map<Long, FormSection> sectionsById) {
        List<Question> questions = loadQuestionPort.listByFormId(formId);
        if (questions.isEmpty()) {
            return;
        }

        Set<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(questionIds);
        if (options.isEmpty()) {
            return;
        }

        Map<Long, Long> questionIdToSectionId = questions.stream()
            .filter(q -> q.getFormSection() != null)
            .collect(Collectors.toMap(Question::getId, q -> q.getFormSection().getId()));

        for (QuestionOption option : options) {
            Long nextSectionId = option.getNextSectionId();
            if (nextSectionId == null) {
                continue;
            }
            Long currentSectionId = questionIdToSectionId.get(option.getQuestion().getId());
            if (currentSectionId == null) {
                continue;
            }
            FormSection current = sectionsById.get(currentSectionId);
            FormSection target = sectionsById.get(nextSectionId);
            if (current == null || target == null) {
                continue;
            }
            if (target.getOrderNo() <= current.getOrderNo()) {
                throw new FormDomainException(FormErrorCode.INVALID_NEXT_SECTION_BACKWARD);
            }
        }
    }
}
