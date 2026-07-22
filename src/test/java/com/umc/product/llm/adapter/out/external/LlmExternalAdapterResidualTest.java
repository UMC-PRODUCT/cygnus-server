package com.umc.product.llm.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;

import com.umc.product.llm.application.port.in.dto.ChatCompleteCommand;
import com.umc.product.llm.application.port.in.dto.ChatCompletionResult;
import com.umc.product.llm.application.port.out.ChatCompletionPort;
import com.umc.product.llm.domain.exception.LlmDomainException;

@DisplayName("LLM 외부 adapter 잔여 계약")
class LlmExternalAdapterResidualTest {

    @Test
    @DisplayName("mock adapter는 null·짧은 prompt를 그대로 echo하고 긴 prompt만 안전하게 자른다")
    void mock_adapter_echo_경계를_검증한다() {
        MockChatCompletionAdapter sut = new MockChatCompletionAdapter();
        String longPrompt = "x".repeat(81);

        assertThat(sut.complete(ChatCompleteCommand.freeForm(null, null)).text()).isEqualTo("[mock] ");
        assertThat(sut.complete(ChatCompleteCommand.freeForm("system", "hello")).text()).isEqualTo("[mock] hello");
        assertThat(sut.complete(ChatCompleteCommand.freeForm("system", longPrompt)).text())
            .isEqualTo("[mock] " + "x".repeat(80) + "...");
        assertThat(sut.providerName()).isEqualTo("mock");
    }

    @Test
    @DisplayName("fallback config는 활성 adapter가 없을 때 mock port를 제공한다")
    void fallback_adapter를_제공한다() {
        ChatCompletionPort port = new LlmFallbackConfig().fallbackChatCompletionPort(properties("unknown"));

        assertThat(port).isInstanceOf(MockChatCompletionAdapter.class);
    }

    @Test
    @DisplayName("OpenAI adapter는 응답을 정규화하고 외부 실패를 도메인 예외로 변환한다")
    void OpenAI_adapter_성공과_실패를_검증한다() {
        OpenAiChatModel model = mock(OpenAiChatModel.class);
        SpringAiOpenAiChatCompletionAdapter sut =
            new SpringAiOpenAiChatCompletionAdapter(model, properties("openai"));
        ChatResponse response = response(" answer ");
        given(model.call(any(Prompt.class))).willReturn(response)
            .willThrow(new IllegalStateException("openai fail"));

        ChatCompletionResult result = sut.complete(ChatCompleteCommand.freeFormWithMaxTokens(null, null, 16));

        assertThat(result.text()).isEqualTo("answer");
        assertThat(result.provider()).isEqualTo("openai");
        assertThat(sut.providerName()).isEqualTo("openai");
        assertThatThrownBy(() -> sut.complete(ChatCompleteCommand.freeForm("s", "u")))
            .isInstanceOf(LlmDomainException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Vertex Gemini adapter는 응답을 정규화하고 외부 실패를 도메인 예외로 변환한다")
    void Vertex_Gemini_adapter_성공과_실패를_검증한다() {
        VertexAiGeminiChatModel model = mock(VertexAiGeminiChatModel.class);
        SpringAiGeminiChatCompletionAdapter sut =
            new SpringAiGeminiChatCompletionAdapter(model, properties("vertexai-gemini"));
        ChatResponse response = response(" answer ");
        given(model.call(any(Prompt.class))).willReturn(response)
            .willThrow(new IllegalStateException("vertex fail"));

        ChatCompletionResult result = sut.complete(ChatCompleteCommand.freeForm("system", "user"));

        assertThat(result.text()).isEqualTo("answer");
        assertThat(result.provider()).isEqualTo("vertexai-gemini");
        assertThat(sut.providerName()).isEqualTo("vertexai-gemini");
        assertThatThrownBy(() -> sut.complete(ChatCompleteCommand.freeForm("s", "u")))
            .isInstanceOf(LlmDomainException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Google GenAI adapter는 null 응답을 빈 문자열로 처리하고 외부 실패를 도메인 예외로 변환한다")
    void Google_GenAI_adapter_null_응답과_실패를_검증한다() {
        GoogleGenAiChatModel model = mock(GoogleGenAiChatModel.class);
        SpringAiGoogleGenAiChatCompletionAdapter sut =
            new SpringAiGoogleGenAiChatCompletionAdapter(model, properties("google-genai"));
        ChatResponse responseWithoutResult = mock(ChatResponse.class);
        given(model.call(any(Prompt.class))).willReturn(responseWithoutResult)
            .willThrow(new IllegalStateException("genai fail"));

        ChatCompletionResult result = sut.complete(ChatCompleteCommand.freeForm(null, null));

        assertThat(result.text()).isEmpty();
        assertThat(result.provider()).isEqualTo("google-genai");
        assertThat(sut.providerName()).isEqualTo("google-genai");
        assertThatThrownBy(() -> sut.complete(ChatCompleteCommand.freeForm("s", "u")))
            .isInstanceOf(LlmDomainException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("응답 result 또는 output이 누락되어도 빈 문자열로 정규화한다")
    void 부분_응답_누락을_빈_문자열로_처리한다() {
        OpenAiChatModel openAi = mock(OpenAiChatModel.class);
        VertexAiGeminiChatModel vertex = mock(VertexAiGeminiChatModel.class);
        ChatResponse missingResult = mock(ChatResponse.class);
        ChatResponse missingOutput = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        given(missingOutput.getResult()).willReturn(generation);
        given(openAi.call(any(Prompt.class))).willReturn(missingResult);
        given(vertex.call(any(Prompt.class))).willReturn(missingOutput);

        assertThat(new SpringAiOpenAiChatCompletionAdapter(openAi, properties("openai"))
            .complete(ChatCompleteCommand.freeForm("s", "u")).text()).isEmpty();
        assertThat(new SpringAiGeminiChatCompletionAdapter(vertex, properties("vertexai-gemini"))
            .complete(ChatCompleteCommand.freeForm("s", "u")).text()).isEmpty();
    }

    private ChatResponse response(String text) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage output = mock(AssistantMessage.class);
        given(response.getResult()).willReturn(generation);
        given(generation.getOutput()).willReturn(output);
        given(output.getText()).willReturn(text);
        return response;
    }

    private LlmProperties properties(String provider) {
        return new LlmProperties(provider, "test-model", 0.0, 32, null, null, null);
    }
}
