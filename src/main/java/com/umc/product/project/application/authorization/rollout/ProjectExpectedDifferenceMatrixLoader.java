package com.umc.product.project.application.authorization.rollout;

public final class ProjectExpectedDifferenceMatrixLoader {

    private final LoadedProjectExpectedDifferenceMatrix matrix;

    public ProjectExpectedDifferenceMatrixLoader() {
        matrix = new LoadedProjectExpectedDifferenceMatrix(
            new ProjectExpectedDifferenceJsonParser().parseClasspath());
    }

    public ProjectExpectedDifferenceMatrix load() {
        return matrix;
    }
}
