package com.umc.product.project.application.authorization.rollout;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

public final class ProjectAuthorizationAuxiliaryFailureReporter {

    private static final Logger log = LoggerFactory.getLogger("project_authorization");
    private static final System.Logger fallback = System.getLogger("project_authorization_fallback");
    private static final String EVENT = "project_authorization_auxiliary_failure";

    private ProjectAuthorizationAuxiliaryFailureReporter() {
    }

    public static void warn(
        ProjectAuthorizationAuxiliaryFailureCode failureCode,
        ProjectPolicyAction action
    ) {
        try {
            log.warn(
                EVENT,
                kv("failureCode", failureCode.name()),
                kv("action", action.id())
            );
        } catch (RuntimeException loggingFailure) {
            warnFallback(failureCode, action);
        }
    }

    private static void warnFallback(
        ProjectAuthorizationAuxiliaryFailureCode failureCode,
        ProjectPolicyAction action
    ) {
        try {
            fallback.log(
                System.Logger.Level.WARNING,
                EVENT + " failureCode=" + failureCode.name() + " action=" + action.id()
            );
        } catch (RuntimeException fallbackFailure) {
            return;
        }
    }
}
