package com.umc.product.project.application.authorization;

import java.util.Comparator;
import java.util.List;

public final class ProjectPolicyActionMatrix {

    private ProjectPolicyActionMatrix() {
    }

    public static String render(List<ProjectPolicySurfaceDescriptor> surfaces) {
        StringBuilder matrix = new StringBuilder();
        matrix.append("# Project Policy Action Matrix\n\n");
        matrix.append("| Surface | Handler | Type | Action | Module | Gate |\n");
        matrix.append("|---|---|---|---|---|---|\n");
        surfaces.stream()
            .sorted(Comparator.comparing(ProjectPolicySurfaceDescriptor::id))
            .forEach(surface -> matrix.append("| ")
                .append(surface.id()).append(" | ")
                .append(surface.handler()).append(" | ")
                .append(surface.type()).append(" | ")
                .append(surface.action().id()).append(" | ")
                .append(surface.module().id()).append(" | ")
                .append(surface.gate()).append(" |\n"));
        return matrix.toString();
    }

    public static void main(String[] args) {
        System.out.print(render(ProjectPolicySurfaceCatalog.surfaces()));
    }
}
