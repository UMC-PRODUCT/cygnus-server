package com.umc.product.demoday.application.port.out;

import java.time.Instant;

public interface IssueDemodayParticipantTokenPort {

    String issue(Long entryCodeId, Instant expiresAt);
}
