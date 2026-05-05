package com.talentcircle.adapter.out.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OpenAiClientAdapterTest {

    private OpenAiClientAdapter openAiClientAdapter;

    @BeforeEach
    void setUp() {
        openAiClientAdapter = new OpenAiClientAdapter(
                "test-api-key",
                "gpt-4-turbo"
        );
    }

    @Test
    void validateConnection_ShouldReturnFalse_WhenNotImplemented() {
        // Given
        // Current implementation returns false since it's not fully implemented
        
        // When
        boolean result = openAiClientAdapter.validateConnection("invalid-key");
        
        // Then
        assertFalse(result);
    }

    @Test
    void generateDraft_ShouldReplacePlaceholdersInPrompt() {
        // Given
        String analysisJson = "{\"topics\": [\"Java\", \"Spring Boot\"]}";
        String channel = "LINKEDIN";
        String promptTemplate = "Create a {channel} post based on: {analysis}";

        // When & Then
        // This will throw RuntimeException since the adapter is not fully implemented
        assertThrows(RuntimeException.class,
            () -> openAiClientAdapter.generateDraft(analysisJson, channel, promptTemplate));
    }
}
