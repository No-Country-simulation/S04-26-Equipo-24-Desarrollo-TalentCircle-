package com.talentcircle.adapter.out.llm;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackLlmClientAdapterTest {

    @Mock
    private OpenAiClientAdapter openAiAdapter;

    @Mock
    private ClaudeClientAdapter claudeAdapter;

    private FallbackLlmClientAdapter fallbackAdapter;

    @BeforeEach
    void setUp() {
        fallbackAdapter = new FallbackLlmClientAdapter(openAiAdapter, claudeAdapter, "openai");
    }

    @Test
    void analyzeActivity_ShouldUsePrimary_WhenItSucceeds() {
        AiAnalysis expected = new AiAnalysis();
        expected.setId("test-id");
        when(openAiAdapter.analyzeActivity(any(), anyString())).thenReturn(expected);

        AiAnalysis result = fallbackAdapter.analyzeActivity(List.of(), "prompt");

        assertSame(expected, result);
        verify(openAiAdapter).analyzeActivity(any(), anyString());
        verify(claudeAdapter, never()).analyzeActivity(any(), anyString());
    }

    @Test
    void analyzeActivity_ShouldFallbackToSecondary_WhenPrimaryFails() {
        AiAnalysis fallbackResult = new AiAnalysis();
        fallbackResult.setId("fallback-id");
        when(openAiAdapter.analyzeActivity(any(), anyString())).thenThrow(new RuntimeException("API error"));
        when(claudeAdapter.analyzeActivity(any(), anyString())).thenReturn(fallbackResult);

        AiAnalysis result = fallbackAdapter.analyzeActivity(List.of(), "prompt");

        assertSame(fallbackResult, result);
        verify(openAiAdapter).analyzeActivity(any(), anyString());
        verify(claudeAdapter).analyzeActivity(any(), anyString());
    }

    @Test
    void analyzeActivity_ShouldThrow_WhenBothFail() {
        when(openAiAdapter.analyzeActivity(any(), anyString())).thenThrow(new RuntimeException("Primary error"));
        when(claudeAdapter.analyzeActivity(any(), anyString())).thenThrow(new RuntimeException("Secondary error"));

        assertThrows(RuntimeException.class,
            () -> fallbackAdapter.analyzeActivity(List.of(), "prompt"));
        verify(openAiAdapter).analyzeActivity(any(), anyString());
        verify(claudeAdapter).analyzeActivity(any(), anyString());
    }

    @Test
    void generateDraft_ShouldUsePrimary_WhenItSucceeds() {
        when(openAiAdapter.generateDraft(anyString(), anyString(), anyString())).thenReturn("draft content");

        String result = fallbackAdapter.generateDraft("json", "LINKEDIN", "template");

        assertEquals("draft content", result);
        verify(openAiAdapter).generateDraft(anyString(), anyString(), anyString());
        verify(claudeAdapter, never()).generateDraft(anyString(), anyString(), anyString());
    }

    @Test
    void generateDraft_ShouldFallbackToSecondary_WhenPrimaryFails() {
        when(openAiAdapter.generateDraft(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("API error"));
        when(claudeAdapter.generateDraft(anyString(), anyString(), anyString())).thenReturn("fallback draft");

        String result = fallbackAdapter.generateDraft("json", "LINKEDIN", "template");

        assertEquals("fallback draft", result);
        verify(openAiAdapter).generateDraft(anyString(), anyString(), anyString());
        verify(claudeAdapter).generateDraft(anyString(), anyString(), anyString());
    }

    @Test
    void validateConnection_ShouldUsePrimary_WhenItSucceeds() {
        when(openAiAdapter.validateConnection(anyString())).thenReturn(true);

        boolean result = fallbackAdapter.validateConnection("key");

        assertTrue(result);
        verify(openAiAdapter).validateConnection(anyString());
        verify(claudeAdapter, never()).validateConnection(anyString());
    }

    @Test
    void validateConnection_ShouldFallback_WhenPrimaryFails() {
        when(openAiAdapter.validateConnection(anyString())).thenThrow(new RuntimeException("API error"));
        when(claudeAdapter.validateConnection(anyString())).thenReturn(true);

        boolean result = fallbackAdapter.validateConnection("key");

        assertTrue(result);
        verify(openAiAdapter).validateConnection(anyString());
        verify(claudeAdapter).validateConnection(anyString());
    }

    @Test
    void validateConnection_ShouldReturnFalse_WhenBothFail() {
        when(openAiAdapter.validateConnection(anyString())).thenThrow(new RuntimeException("Primary error"));
        when(claudeAdapter.validateConnection(anyString())).thenThrow(new RuntimeException("Secondary error"));

        boolean result = fallbackAdapter.validateConnection("key");

        assertFalse(result);
        verify(openAiAdapter).validateConnection(anyString());
        verify(claudeAdapter).validateConnection(anyString());
    }

    @Test
    void shouldUseClaudeAsPrimary_WhenProviderIsAnthropic() {
        FallbackLlmClientAdapter claudePrimary =
            new FallbackLlmClientAdapter(openAiAdapter, claudeAdapter, "anthropic");

        AiAnalysis expected = new AiAnalysis();
        expected.setId("claude-analysis");
        when(claudeAdapter.analyzeActivity(any(), anyString())).thenReturn(expected);

        AiAnalysis result = claudePrimary.analyzeActivity(List.of(), "prompt");

        assertSame(expected, result);
        verify(claudeAdapter).analyzeActivity(any(), anyString());
        verify(openAiAdapter, never()).analyzeActivity(any(), anyString());
    }

    @Test
    void shouldFallbackToOpenAi_WhenClaudeIsPrimaryAndFails() {
        FallbackLlmClientAdapter claudePrimary =
            new FallbackLlmClientAdapter(openAiAdapter, claudeAdapter, "anthropic");

        AiAnalysis expected = new AiAnalysis();
        expected.setId("openai-fallback");
        when(claudeAdapter.analyzeActivity(any(), anyString())).thenThrow(new RuntimeException("Claude down"));
        when(openAiAdapter.analyzeActivity(any(), anyString())).thenReturn(expected);

        AiAnalysis result = claudePrimary.analyzeActivity(List.of(), "prompt");

        assertSame(expected, result);
        verify(claudeAdapter).analyzeActivity(any(), anyString());
        verify(openAiAdapter).analyzeActivity(any(), anyString());
    }
}
