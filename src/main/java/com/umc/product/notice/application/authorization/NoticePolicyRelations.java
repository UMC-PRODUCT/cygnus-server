package com.umc.product.notice.application.authorization;

record NoticePolicyRelations(
    boolean superAdmin,
    boolean activeCentralCore,
    boolean targetChallenger,
    boolean activeRoleCanReadTarget,
    boolean author,
    boolean activeManagerForTarget,
    boolean activeCreatorForTarget
) {
}
