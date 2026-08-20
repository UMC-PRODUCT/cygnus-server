package com.umc.product.demoday.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.DeleteDemodayTestDataPort;
import com.umc.product.demoday.application.port.out.dto.DemodayTestDataDeletionCounts;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "demoday.test-data-reset", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemodayTestDataCleanupPersistenceAdapter implements DeleteDemodayTestDataPort {

    private final EntityManager entityManager;

    @Override
    public DemodayTestDataDeletionCounts deleteAll() {
        int deletedStamps = entityManager.createNativeQuery("DELETE FROM demoday_stamp").executeUpdate();
        int deletedVotes = entityManager.createNativeQuery("DELETE FROM demoday_vote").executeUpdate();
        int deletedEntryCodes = entityManager.createNativeQuery("DELETE FROM demoday_entry_code").executeUpdate();
        int deletedBooths = entityManager.createNativeQuery("DELETE FROM demoday_booth").executeUpdate();
        int deletedPolls = entityManager.createNativeQuery("DELETE FROM demoday_poll").executeUpdate();

        return new DemodayTestDataDeletionCounts(
            deletedPolls,
            deletedBooths,
            deletedEntryCodes,
            deletedStamps,
            deletedVotes
        );
    }
}
