package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.PipelineConfig;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.PipelineConfigRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftGeneratorServiceTest {

    @Mock private WeeklyExecutionRepository executionRepository;
    @Mock private AiAnalysisRepository analysisRepository;
    @Mock private DraftRepository draftRepository;
    @Mock private LlmClientPort llmClient;
    @Mock private PipelineConfigRepository configRepository;

    private DraftGeneratorService generatorService;

    private WeeklyExecution testExecution;
    private AiAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        // Construct manually — no more @Value fields, prompts come from configRepository
        generatorService = new DraftGeneratorService(
                executionRepository, analysisRepository, draftRepository, llmClient, configRepository);

        testExecution = new WeeklyExecution();
        testExecution.setId("exec-123");
        testExecution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);

        testAnalysis = new AiAnalysis();
        testAnalysis.setId("analysis-123");
        testAnalysis.setExecutiveSummary("Test summary");
        testAnalysis.setTopTopics("[\"topic1\", \"topic2\"]");
        testAnalysis.setRelevanceScores("{\"activity1\": 0.9}");

        // Default: no DB config → use built-in defaults
        Mockito.lenient().when(configRepository.findSingleton()).thenReturn(Optional.empty());

        // Default LLM stub
        Mockito.lenient().when(llmClient.generateDraft(anyString(), anyString(), anyString()))
                .thenReturn("Generated content");
    }

    @Test
    void generateDrafts_shouldGenerateThreeDraftsForAllChannels() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.of(testAnalysis));
        when(draftRepository.save(any(Draft.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Draft> drafts = generatorService.generateDrafts("exec-123");

        assertNotNull(drafts);
        assertEquals(3, drafts.size());

        assertNotNull(drafts.stream().filter(d -> d.getChannel() == Draft.Channel.NEWSLETTER).findFirst().orElse(null));
        assertNotNull(drafts.stream().filter(d -> d.getChannel() == Draft.Channel.LINKEDIN).findFirst().orElse(null));
        assertNotNull(drafts.stream().filter(d -> d.getChannel() == Draft.Channel.TWITTER).findFirst().orElse(null));

        verify(draftRepository, times(3)).save(any(Draft.class));
    }

    @Test
    void generateDrafts_usesPromptsFromDbWhenConfigured() {
        PipelineConfig config = new PipelineConfig();
        config.setNewsletterPrompt("Prompt newsletter desde BD");
        config.setLinkedInPrompt("Prompt linkedin desde BD");
        config.setTwitterPrompt("Prompt twitter desde BD");

        when(configRepository.findSingleton()).thenReturn(Optional.of(config));
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.of(testAnalysis));
        when(draftRepository.save(any(Draft.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Draft> drafts = generatorService.generateDrafts("exec-123");

        assertEquals(3, drafts.size());
        // Verify LLM was called with the DB prompts
        verify(llmClient).generateDraft(anyString(), eq("NEWSLETTER"), eq("Prompt newsletter desde BD"));
        verify(llmClient).generateDraft(anyString(), eq("LINKEDIN"),   eq("Prompt linkedin desde BD"));
        verify(llmClient).generateDraft(anyString(), eq("TWITTER"),    eq("Prompt twitter desde BD"));
    }

    @Test
    void generateDrafts_shouldThrowExceptionWhenExecutionNotFound() {
        when(executionRepository.findById("invalid-exec")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                generatorService.generateDrafts("invalid-exec"));

        assertEquals("Execution not found: invalid-exec", exception.getMessage());
        verify(analysisRepository, never()).findByExecutionId(anyString());
        verify(draftRepository, never()).save(any(Draft.class));
    }

    @Test
    void generateDrafts_shouldThrowExceptionWhenNoAnalysisFound() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                generatorService.generateDrafts("exec-123"));

        assertEquals("No AI analysis found for execution: exec-123", exception.getMessage());
        verify(draftRepository, never()).save(any(Draft.class));
    }

    @Test
    void generateDrafts_shouldTruncateTwitterContent() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.of(testAnalysis));

        String longContent = "a".repeat(300);
        when(llmClient.generateDraft(anyString(), eq("TWITTER"), anyString())).thenReturn(longContent);
        when(draftRepository.save(any(Draft.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Draft> drafts = generatorService.generateDrafts("exec-123");
        Draft twitterDraft = drafts.stream()
                .filter(d -> d.getChannel() == Draft.Channel.TWITTER)
                .findFirst().orElse(null);

        assertNotNull(twitterDraft);
        assertTrue(twitterDraft.getContent().length() <= 280);
        assertTrue(twitterDraft.getContent().endsWith("..."));
    }

    @Test
    void generateDrafts_shouldNotTruncateNewsletterContent() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.of(testAnalysis));

        String longNewsletter = "n".repeat(5000);
        when(llmClient.generateDraft(anyString(), eq("NEWSLETTER"), anyString())).thenReturn(longNewsletter);
        when(draftRepository.save(any(Draft.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Draft> drafts = generatorService.generateDrafts("exec-123");
        Draft newsletterDraft = drafts.stream()
                .filter(d -> d.getChannel() == Draft.Channel.NEWSLETTER)
                .findFirst().orElse(null);

        assertNotNull(newsletterDraft);
        assertEquals(longNewsletter.length(), newsletterDraft.getContent().length());
    }

    @Test
    void generateDrafts_shouldSetAiScore() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.of(testAnalysis));
        when(draftRepository.save(any(Draft.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Draft> drafts = generatorService.generateDrafts("exec-123");

        for (Draft draft : drafts) {
            assertNotNull(draft.getAiScore());
            assertEquals(0.85, draft.getAiScore(), 0.001);
        }
    }
}
