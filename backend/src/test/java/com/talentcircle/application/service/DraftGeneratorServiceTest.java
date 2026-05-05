package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftGeneratorServiceTest {

    @Mock
    private WeeklyExecutionRepository executionRepository;

    @Mock
    private AiAnalysisRepository analysisRepository;

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private LlmClientPort llmClient;

    @InjectMocks
    private DraftGeneratorService generatorService;

    private WeeklyExecution testExecution;
    private AiAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        testExecution = new WeeklyExecution();
        testExecution.setId("exec-123");
        testExecution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);

        testAnalysis = new AiAnalysis();
        testAnalysis.setId("analysis-123");
        testAnalysis.setExecutiveSummary("Test summary");
        testAnalysis.setTopTopics("[\"topic1\", \"topic2\"]");
        testAnalysis.setRelevanceScores("{\"activity1\": 0.9}");

        // Set prompt templates directly since @Value doesn't work in unit tests
        try {
            java.lang.reflect.Field newsletterField = DraftGeneratorService.class.getDeclaredField("newsletterPrompt");
            newsletterField.setAccessible(true);
            newsletterField.set(generatorService, "Generate a newsletter draft");

            java.lang.reflect.Field linkedinField = DraftGeneratorService.class.getDeclaredField("linkedinPrompt");
            linkedinField.setAccessible(true);
            linkedinField.set(generatorService, "Generate a LinkedIn post");

            java.lang.reflect.Field twitterField = DraftGeneratorService.class.getDeclaredField("twitterPrompt");
            twitterField.setAccessible(true);
            twitterField.set(generatorService, "Generate a Twitter post");
        } catch (Exception e) {
            fail("Failed to set prompt fields: " + e.getMessage());
        }

        // Use lenient stubbing for llmClient
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

        Draft newsletter = drafts.stream()
                .filter(d -> d.getChannel() == Draft.Channel.NEWSLETTER)
                .findFirst().orElse(null);
        assertNotNull(newsletter);
        assertEquals(Draft.DraftStatus.PENDING, newsletter.getStatus());

        Draft linkedin = drafts.stream()
                .filter(d -> d.getChannel() == Draft.Channel.LINKEDIN)
                .findFirst().orElse(null);
        assertNotNull(linkedin);

        Draft twitter = drafts.stream()
                .filter(d -> d.getChannel() == Draft.Channel.TWITTER)
                .findFirst().orElse(null);
        assertNotNull(twitter);

        verify(draftRepository, times(3)).save(any(Draft.class));
    }

    @Test
    void generateDrafts_shouldThrowExceptionWhenExecutionNotFound() {
        when(executionRepository.findById("invalid-exec")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                generatorService.generateDrafts("invalid-exec")
        );

        assertEquals("Execution not found: invalid-exec", exception.getMessage());
        verify(analysisRepository, never()).findByExecutionId(anyString());
        verify(draftRepository, never()).save(any(Draft.class));
    }

    @Test
    void generateDrafts_shouldThrowExceptionWhenNoAnalysisFound() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                generatorService.generateDrafts("exec-123")
        );

        assertEquals("No AI analysis found for execution: exec-123", exception.getMessage());
        verify(draftRepository, never()).save(any(Draft.class));
    }

    @Test
    void generateDrafts_shouldTruncateTwitterContent() {
        when(executionRepository.findById("exec-123")).thenReturn(Optional.of(testExecution));
        when(analysisRepository.findByExecutionId("exec-123")).thenReturn(Optional.of(testAnalysis));

        String longContent = "a".repeat(300);
        when(llmClient.generateDraft(anyString(), eq("TWITTER"), anyString()))
                .thenReturn(longContent);
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
        when(llmClient.generateDraft(anyString(), eq("NEWSLETTER"), anyString()))
                .thenReturn(longNewsletter);
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
