package com.umc.product.project.application.authorization.rollout;

import java.util.Objects;

public sealed interface ProjectAuthorizationEvaluationPoint
    permits ProjectAuthorizationEvaluationPoint.Surface, ProjectAuthorizationEvaluationPoint.Internal {

    static ProjectAuthorizationEvaluationPoint surface(ProjectAuthorizationSurface surface) {
        return new Surface(surface);
    }

    static ProjectAuthorizationEvaluationPoint internal(ProjectAuthorizationInternalOrigin origin) {
        return new Internal(origin);
    }

    record Surface(ProjectAuthorizationSurface surface) implements ProjectAuthorizationEvaluationPoint {
        public Surface {
            Objects.requireNonNull(surface);
        }

        public com.umc.product.project.application.authorization.ProjectPolicySurfaceIdentity identity() {
            return surface.identity();
        }
    }

    record Internal(ProjectAuthorizationInternalOrigin origin) implements ProjectAuthorizationEvaluationPoint {
        public Internal {
            Objects.requireNonNull(origin);
        }
    }
}
