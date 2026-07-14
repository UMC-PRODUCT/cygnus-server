package com.umc.product.project.application.authorization.rollout;

public final class InitialProjectExpectedDifferenceMatrix {

    private InitialProjectExpectedDifferenceMatrix() {
    }

    public static ProjectExpectedDifferenceMatrix create() {
        return new ProjectExpectedDifferenceMatrixLoader().load();
    }
}
