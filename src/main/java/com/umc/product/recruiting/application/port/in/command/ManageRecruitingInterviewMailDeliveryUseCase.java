package com.umc.product.recruiting.application.port.in.command;

import java.time.Instant;

public interface ManageRecruitingInterviewMailDeliveryUseCase {

    void markRequestMailSent(Long applicationId, Instant sentAt);

    void markRequestMailFailed(Long applicationId, String error);
}
