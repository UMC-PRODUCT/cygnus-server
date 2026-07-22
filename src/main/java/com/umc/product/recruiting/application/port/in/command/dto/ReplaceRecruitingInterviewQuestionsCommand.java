package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

public final class ReplaceRecruitingInterviewQuestionsCommand {

    private ReplaceRecruitingInterviewQuestionsCommand() {
    }

    public record Round(Long roundId, Long requesterMemberId, List<Entry> questions) {

        public Round {
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    public record Application(Long applicationId, Long requesterMemberId, List<Entry> questions) {

        public Application {
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    public record Entry(Long id, String content, Integer orderNo) {
    }
}
