package com.umc.product.form.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageAnswerUseCase;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.ManageVoteUseCase;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormSectionUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.GetQuestionOptionUseCase;
import com.umc.product.form.application.port.in.query.GetQuestionUseCase;
import com.umc.product.form.application.port.in.query.GetVoteUseCase;
import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("Form Port In ownership 계약")
class FormPortInOwnershipContractTest {

    private static final List<Class<?>> PORTS = List.of(
        ManageFormUseCase.class,
        ManageFormSectionUseCase.class,
        ManageQuestionUseCase.class,
        ManageQuestionOptionUseCase.class,
        ManageFormResponseUseCase.class,
        ManageAnswerUseCase.class,
        ManageVoteUseCase.class,
        GetFormUseCase.class,
        GetFormSectionUseCase.class,
        GetQuestionUseCase.class,
        GetQuestionOptionUseCase.class,
        GetFormResponseUseCase.class,
        GetAnswerUseCase.class,
        GetVoteUseCase.class
    );

    @Test
    @DisplayName("모든 public operation은 expected owner와 actor context를 명시적으로 요구한다")
    void 모든_public_operation은_expected_owner와_actor_context를_요구한다() {
        List<Method> methodsWithoutOwnershipBoundary = PORTS.stream()
            .flatMap(port -> List.of(port.getDeclaredMethods()).stream())
            .filter(method -> !hasActorContext(method) || !hasExpectedOwner(method))
            .toList();

        assertThat(methodsWithoutOwnershipBoundary).isEmpty();
    }

    private boolean hasActorContext(Method method) {
        return List.of(method.getParameterTypes()).contains(FormActorContext.class);
    }

    private boolean hasExpectedOwner(Method method) {
        for (Type type : method.getGenericParameterTypes()) {
            if (type == FormOwnerReference.class || type == FormOwnerReferenceFactory.class) {
                return true;
            }
            if (type instanceof ParameterizedType parameterizedType
                && List.of(parameterizedType.getActualTypeArguments()).contains(FormOwnerReference.class)) {
                return true;
            }
        }
        return false;
    }
}
