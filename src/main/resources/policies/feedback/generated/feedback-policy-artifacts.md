# Feedback Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `feedback-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `b863e348bc6bf16d66278ba1004a1c49eb65d63d950e12659c75208f03e7a20a`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:GET /api/v1/user-feedbacks/templates | com.umc.product.feedback.adapter.in.web.UserFeedbackController#getTemplate | REST | feedback-template:resolve | feedback-resource | DIRECT |
| rest:POST /api/v1/user-feedbacks/responses | com.umc.product.feedback.adapter.in.web.UserFeedbackController#submit | REST | feedback-response:submit | feedback-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| feedback-resource | feedback.resolve.admin | ALLOW | feedback-template:resolve | EQ(ATTRIBUTE(relation.activeCentralMemberInTargetGisu), true) | feedback.targetType=ADMIN |
| feedback-resource | feedback.resolve.experienced-generation-ten-plan | ALLOW | feedback-template:resolve | EQ(ATTRIBUTE(relation.generationTenPlanChallenger), true) | feedback.targetType=EXPERIENCED_CHALLENGER |
| feedback-resource | feedback.resolve.experienced-history | ALLOW | feedback-template:resolve | ALL(EQ(ATTRIBUTE(relation.activeChallengerInTargetGisu), true), EQ(ATTRIBUTE(relation.hasPreviousGisu), true)) | feedback.targetType=EXPERIENCED_CHALLENGER |
| feedback-resource | feedback.resolve.new-challenger | ALLOW | feedback-template:resolve | EQ(ATTRIBUTE(relation.activeChallengerInTargetGisu), true) | feedback.targetType=NEW_CHALLENGER |
| feedback-resource | feedback.submit.admin | ALLOW | feedback-response:submit | ALL(EQ(ATTRIBUTE(relation.activeCentralMemberInTargetGisu), true), EQ(ATTRIBUTE(resource.feedback.targetType), ADMIN)) |  |
| feedback-resource | feedback.submit.experienced | ALLOW | feedback-response:submit | ALL(ANY(EQ(ATTRIBUTE(relation.generationTenPlanChallenger), true), EQ(ATTRIBUTE(relation.hasPreviousGisu), true)), EQ(ATTRIBUTE(relation.activeCentralMemberInTargetGisu), false), EQ(ATTRIBUTE(relation.activeChallengerInTargetGisu), true), EQ(ATTRIBUTE(resource.feedback.targetType), EXPERIENCED_CHALLENGER)) |  |
| feedback-resource | feedback.submit.new-challenger | ALLOW | feedback-response:submit | ALL(EQ(ATTRIBUTE(relation.activeCentralMemberInTargetGisu), false), EQ(ATTRIBUTE(relation.activeChallengerInTargetGisu), true), EQ(ATTRIBUTE(relation.generationTenPlanChallenger), false), EQ(ATTRIBUTE(relation.hasPreviousGisu), false), EQ(ATTRIBUTE(resource.feedback.targetType), NEW_CHALLENGER)) |  |
