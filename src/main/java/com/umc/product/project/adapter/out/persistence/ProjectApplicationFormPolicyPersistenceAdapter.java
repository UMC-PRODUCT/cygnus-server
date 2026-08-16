package com.umc.product.project.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPolicyPort;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectApplicationFormPolicyPersistenceAdapter
    implements LoadProjectApplicationFormPolicyPort, SaveProjectApplicationFormPolicyPort {

    private final ProjectApplicationFormPolicyJpaRepository repository;

    @Override
    public List<ProjectApplicationFormPolicy> listByApplicationFormId(Long applicationFormId) {
        return repository.findAllByApplicationFormId(applicationFormId);
    }

    @Override
    public Map<Long, List<ProjectApplicationFormPolicy>> listByApplicationFormIds(
        Collection<Long> applicationFormIds
    ) {
        if (applicationFormIds == null || applicationFormIds.isEmpty()) {
            return Map.of();
        }

        return repository.findAllByApplicationFormIdIn(applicationFormIds).stream()
            .collect(Collectors.groupingBy(policy -> policy.getApplicationForm().getId()));
    }

    @Override
    public ProjectApplicationFormPolicy save(ProjectApplicationFormPolicy policy) {
        return repository.save(policy);
    }

    @Override
    public List<ProjectApplicationFormPolicy> saveAll(List<ProjectApplicationFormPolicy> policies) {
        return repository.saveAll(policies);
    }

    @Override
    public void delete(ProjectApplicationFormPolicy policy) {
        repository.delete(policy);
    }

    @Override
    @Transactional
    public void deleteByFormSectionId(Long formSectionId) {
        repository.deleteByFormSectionId(formSectionId);
    }

    @Override
    @Transactional
    public void deleteAllByApplicationFormId(Long applicationFormId) {
        repository.deleteAllByApplicationFormId(applicationFormId);
    }
}
