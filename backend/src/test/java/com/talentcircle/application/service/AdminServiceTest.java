package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.PipelineConfig;
import com.talentcircle.domain.model.User;
import com.talentcircle.domain.port.in.AdminUseCase;
import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.PipelineConfigRepository;
import com.talentcircle.domain.port.out.UserRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private CommunitySourceRepository sourceRepository;

    @Mock
    private PipelineConfigRepository configRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeeklyExecutionRepository executionRepository;

    @Mock
    private PipelineOrchestratorUseCase pipelineOrchestrator;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(sourceRepository, configRepository, userRepository, executionRepository, pipelineOrchestrator);
    }

    @Test
    void getSources_ShouldReturnAllActiveSources() {
        // Given
        CommunitySource source1 = new CommunitySource();
        source1.setId("1");
        source1.setName("Discord");
        source1.setActive(true);

        when(sourceRepository.findAll()).thenReturn(Arrays.asList(source1));

        // When
        List<AdminUseCase.SourceDto> result = adminService.getSources();

        // Then
        assertEquals(1, result.size());
        assertEquals("Discord", result.get(0).name());
        assertTrue(result.get(0).active());
    }

    @Test
    void createSource_ShouldCreateNewSource() {
        // Given
        AdminUseCase.CreateSourceRequest request = new AdminUseCase.CreateSourceRequest(
                "Slack", "SLACK", "https://slack.com/api", "encrypted-key"
        );

        when(sourceRepository.save(any(CommunitySource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AdminUseCase.SourceDto result = adminService.createSource(request);

        // Then
        assertEquals("Slack", result.name());
        assertEquals("SLACK", result.type());
        assertTrue(result.active());
    }

    @Test
    void deleteSource_ShouldMarkSourceAsInactive() {
        // Given
        CommunitySource source = new CommunitySource();
        source.setId("123");
        source.setActive(true);

        when(sourceRepository.findById("123")).thenReturn(Optional.of(source));
        when(sourceRepository.save(any(CommunitySource.class))).thenReturn(source);

        // When
        adminService.deleteSource("123");

        // Then
        assertFalse(source.isActive());
        verify(sourceRepository).save(source);
    }

    @Test
    void getConfig_ShouldReturnConfig_WhenExists() {
        // Given
        PipelineConfig config = new PipelineConfig();
        config.setLlmProvider("openai");
        config.setLlmModel("gpt-4-turbo");

        when(configRepository.findSingleton()).thenReturn(Optional.of(config));

        // When
        AdminUseCase.ConfigDto result = adminService.getConfig();

        // Then
        assertEquals("openai", result.llmProvider());
        assertEquals("gpt-4-turbo", result.llmModel());
    }

    @Test
    void updateConfig_ShouldUpdateExistingConfig() {
        // Given
        PipelineConfig config = new PipelineConfig();
        when(configRepository.findSingleton()).thenReturn(Optional.of(config));
        when(configRepository.save(any(PipelineConfig.class))).thenReturn(config);

        AdminUseCase.UpdateConfigRequest request = new AdminUseCase.UpdateConfigRequest(
                "anthropic", "claude-3-5-sonnet", "newsletter prompt", "linkedin prompt", "twitter prompt", 15, "0 18 * * FRI"
        );

        // When
        AdminUseCase.ConfigDto result = adminService.updateConfig(request);

        // Then
        assertEquals("anthropic", result.llmProvider());
        assertEquals("claude-3-5-sonnet", result.llmModel());
        assertEquals(15, result.maxItemsPerChannel());
    }
}
