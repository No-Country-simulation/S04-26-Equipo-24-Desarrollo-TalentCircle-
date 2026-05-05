package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DraftGeneratorService implements DraftGeneratorUseCase {

    private final WeeklyExecutionRepository executionRepository;
    private final AiAnalysisRepository analysisRepository;
    private final DraftRepository draftRepository;
    private final LlmClientPort llmClient;

    @Value("${app.pipeline.prompts.newsletter:Generate a newsletter draft}")
    private String newsletterPrompt;

    @Value("${app.pipeline.prompts.linkedin:Generate a LinkedIn post}")
    private String linkedinPrompt;

    @Value("${app.pipeline.prompts.twitter:Generate a Twitter post}")
    private String twitterPrompt;

    public DraftGeneratorService(WeeklyExecutionRepository executionRepository,
                                 AiAnalysisRepository analysisRepository,
                                 DraftRepository draftRepository,
                                 LlmClientPort llmClient) {
        this.executionRepository = executionRepository;
        this.analysisRepository = analysisRepository;
        this.draftRepository = draftRepository;
        this.llmClient = llmClient;
    }

    @Override
    public List<Draft> generateDrafts(String executionId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        // Get AI analysis for this execution
        AiAnalysis analysis = analysisRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new IllegalStateException("No AI analysis found for execution: " + executionId));

        String analysisJson = buildAnalysisJson(analysis);

        List<Draft> drafts = new ArrayList<>();

        // Generate drafts for each channel
        drafts.add(generateDraftForChannel(execution, analysisJson, Draft.Channel.NEWSLETTER, newsletterPrompt));
        drafts.add(generateDraftForChannel(execution, analysisJson, Draft.Channel.LINKEDIN, linkedinPrompt));
        drafts.add(generateDraftForChannel(execution, analysisJson, Draft.Channel.TWITTER, twitterPrompt));

        // Save all drafts
        drafts.forEach(draft -> {
            draft.setCreatedAt(LocalDateTime.now());
            draftRepository.save(draft);
        });

        return drafts;
    }

    private Draft generateDraftForChannel(WeeklyExecution execution, String analysisJson,
                                         Draft.Channel channel, String promptTemplate) {
        Draft draft = new Draft();
        draft.setId(UUID.randomUUID().toString());
        draft.setExecution(execution);
        draft.setChannel(channel);
        draft.setStatus(Draft.DraftStatus.PENDING);

        // Call LLM to generate content
        String draftContent = llmClient.generateDraft(analysisJson, channel.name(), promptTemplate);

        // Validate length constraints per channel
        draftContent = validateAndTruncateContent(draftContent, channel);

        draft.setContent(draftContent);
        draft.setAiScore(0.85); // Default score, can be updated by AI analyzer

        return draft;
    }

    private String validateAndTruncateContent(String content, Draft.Channel channel) {
        if (content == null) {
            return "";
        }

        int maxLength = getMaxLengthForChannel(channel);
        if (content.length() > maxLength) {
            return content.substring(0, maxLength - 3) + "...";
        }
        return content;
    }

    private int getMaxLengthForChannel(Draft.Channel channel) {
        return switch (channel) {
            case TWITTER -> 280;
            case LINKEDIN -> 3000;
            case NEWSLETTER -> 10000;
        };
    }

    private String buildAnalysisJson(AiAnalysis analysis) {
        return String.format(
                "{\"summary\": \"%s\", \"topics\": %s, \"relevanceScores\": %s}",
                analysis.getExecutiveSummary() != null ? analysis.getExecutiveSummary() : "",
                analysis.getTopTopics() != null ? analysis.getTopTopics() : "[]",
                analysis.getRelevanceScores() != null ? analysis.getRelevanceScores() : "{}"
        );
    }
}
