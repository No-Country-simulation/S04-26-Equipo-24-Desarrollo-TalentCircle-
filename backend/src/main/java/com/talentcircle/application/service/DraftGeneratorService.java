package com.talentcircle.application.service;

import com.talentcircle.adapter.out.llm.LlmClientFactory;
import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.PipelineConfig;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.PipelineConfigRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DraftGeneratorService implements DraftGeneratorUseCase {

    private static final Logger log = LoggerFactory.getLogger(DraftGeneratorService.class);

    // Prompts por defecto si la BD no tiene configuración
    private static final String DEFAULT_NEWSLETTER_PROMPT =
            "Genera un borrador de newsletter semanal en español basado en las siguientes actividades de la comunidad. " +
            "Incluye un resumen ejecutivo, los temas más relevantes y llamadas a la acción.";

    private static final String DEFAULT_LINKEDIN_PROMPT =
            "Genera una publicación profesional para LinkedIn en español basada en las siguientes actividades de la comunidad. " +
            "Tono profesional, máximo 3000 caracteres, incluye hashtags relevantes.";

    private static final String DEFAULT_TWITTER_PROMPT =
            "Genera un tweet en español basado en las siguientes actividades de la comunidad. " +
            "Máximo 280 caracteres, tono dinámico, incluye hashtags.";

    private final WeeklyExecutionRepository executionRepository;
    private final AiAnalysisRepository analysisRepository;
    private final DraftRepository draftRepository;
    private final LlmClientFactory llmClientFactory;
    private final PipelineConfigRepository configRepository;

    public DraftGeneratorService(WeeklyExecutionRepository executionRepository,
                                 AiAnalysisRepository analysisRepository,
                                 DraftRepository draftRepository,
                                 LlmClientFactory llmClientFactory,
                                 PipelineConfigRepository configRepository) {
        this.executionRepository = executionRepository;
        this.analysisRepository  = analysisRepository;
        this.draftRepository     = draftRepository;
        this.llmClientFactory    = llmClientFactory;
        this.configRepository    = configRepository;
    }

    @Override
    public List<Draft> generateDrafts(String executionId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        // Get AI analysis for this execution
        AiAnalysis analysis = analysisRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new IllegalStateException(
                        "No AI analysis found for execution: " + executionId));

        // Load prompts from DB config (fallback to defaults if not configured)
        PipelineConfig config = configRepository.findSingleton().orElse(null);
        String newsletterPrompt = resolvePrompt(config != null ? config.getNewsletterPrompt() : null,
                DEFAULT_NEWSLETTER_PROMPT, "newsletter");
        String linkedinPrompt   = resolvePrompt(config != null ? config.getLinkedInPrompt() : null,
                DEFAULT_LINKEDIN_PROMPT, "linkedin");
        String twitterPrompt    = resolvePrompt(config != null ? config.getTwitterPrompt() : null,
                DEFAULT_TWITTER_PROMPT, "twitter");

        String analysisJson = buildAnalysisJson(analysis);

        List<Draft> drafts = new ArrayList<>();
        drafts.add(generateDraftForChannel(execution, analysisJson, Draft.Channel.NEWSLETTER, newsletterPrompt));
        drafts.add(generateDraftForChannel(execution, analysisJson, Draft.Channel.LINKEDIN,   linkedinPrompt));
        drafts.add(generateDraftForChannel(execution, analysisJson, Draft.Channel.TWITTER,    twitterPrompt));

        drafts.forEach(draft -> {
            draft.setCreatedAt(LocalDateTime.now());
            draftRepository.save(draft);
        });

        log.info("Generados {} borradores para ejecución {}", drafts.size(), executionId);
        return drafts;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Draft generateDraftForChannel(WeeklyExecution execution, String analysisJson,
                                          Draft.Channel channel, String promptTemplate) {
        log.debug("Generando borrador para canal {} con prompt de {} chars",
                channel, promptTemplate.length());

        Draft draft = new Draft();
        draft.setId(UUID.randomUUID().toString());
        draft.setExecution(execution);
        draft.setChannel(channel);
        draft.setStatus(Draft.DraftStatus.PENDING);

        String draftContent = llmClientFactory.getClient().generateDraft(analysisJson, channel.name(), promptTemplate);
        draftContent = validateAndTruncateContent(draftContent, channel);

        draft.setContent(draftContent);
        draft.setAiScore(0.85);

        return draft;
    }

    private String resolvePrompt(String dbPrompt, String defaultPrompt, String channel) {
        if (dbPrompt != null && !dbPrompt.isBlank()) {
            log.debug("Usando prompt de BD para canal {}", channel);
            return dbPrompt;
        }
        log.debug("Usando prompt por defecto para canal {} (no configurado en BD)", channel);
        return defaultPrompt;
    }

    private String validateAndTruncateContent(String content, Draft.Channel channel) {
        if (content == null) return "";
        int maxLength = switch (channel) {
            case TWITTER     -> 280;
            case LINKEDIN    -> 3000;
            case NEWSLETTER  -> 10000;
        };
        if (content.length() > maxLength) {
            return content.substring(0, maxLength - 3) + "...";
        }
        return content;
    }

    private String buildAnalysisJson(AiAnalysis analysis) {
        return String.format(
                "{\"summary\": \"%s\", \"topics\": %s, \"relevanceScores\": %s}",
                analysis.getExecutiveSummary() != null
                        ? analysis.getExecutiveSummary().replace("\"", "\\\"") : "",
                analysis.getTopTopics() != null ? analysis.getTopTopics() : "[]",
                analysis.getRelevanceScores() != null ? analysis.getRelevanceScores() : "{}"
        );
    }
}
