package com.umc.product.community.application.port.in.query.thread.dto;

import java.time.Instant;

public record ThreadLastMessageInfo(String preview, String senderName, Instant createdAt) {
}
