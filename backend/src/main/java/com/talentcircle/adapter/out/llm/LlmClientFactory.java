package com.talentcircle.adapter.out.llm;

import com.talentcircle.domain.port.out.LlmClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the active {@link LlmClientPort} implementation based on
 * the {@code app.llm.provider} configuration property.
 *
 * <p>Supported values:
 * <ul>
 *   <li>{@code openai}      — uses {@link OpenAiClientAdapter} (default)</li>
 *   <li>{@code openrouter}  — uses {@link OpenRouterClientAdapter}</li>
 *   <li>{@code claude}      — uses {@link ClaudeClientAdapter}</li>
 * </ul>
 *
 * <p>Usage in services:
 * <pre>
 *   private final LlmClientPort llm;
 *
 *   public MyService(LlmClientFactory factory) {
 *       this.llm = factory.getClient();
 *   }
 * </pre>
 */
@Component
public class LlmClientFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);

    private final String provider;
    private final OpenAiClientAdapter     openAiAdapter;
    private final OpenRouterClientAdapter openRouterAdapter;
    private final ClaudeClientAdapter     claudeAdapter;

    public LlmClientFactory(
            @Value("${app.llm.provider:openai}") String provider,
            OpenAiClientAdapter     openAiAdapter,
            OpenRouterClientAdapter openRouterAdapter,
            ClaudeClientAdapter     claudeAdapter) {
        this.provider          = provider;
        this.openAiAdapter     = openAiAdapter;
        this.openRouterAdapter = openRouterAdapter;
        this.claudeAdapter     = claudeAdapter;
        log.info("LLM provider configured: {}", provider);
    }

    /**
     * Returns the {@link LlmClientPort} for the configured provider.
     * Falls back to OpenAI if the provider value is unrecognised.
     */
    public LlmClientPort getClient() {
        return switch (provider.toLowerCase().trim()) {
            case "openrouter" -> {
                log.debug("Using OpenRouter LLM client");
                yield openRouterAdapter;
            }
            case "claude", "anthropic" -> {
                log.debug("Using Claude LLM client");
                yield claudeAdapter;
            }
            default -> {
                if (!"openai".equalsIgnoreCase(provider)) {
                    log.warn("Unknown LLM provider '{}', falling back to OpenAI", provider);
                }
                log.debug("Using OpenAI LLM client");
                yield openAiAdapter;
            }
        };
    }
}
