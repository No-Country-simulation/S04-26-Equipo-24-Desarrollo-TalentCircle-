package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.model.PipelineConfig;
import com.talentcircle.domain.model.User;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AdminUseCase;
import com.talentcircle.domain.port.out.CommunitySourceRepository;
import com.talentcircle.domain.port.out.PipelineConfigRepository;
import com.talentcircle.domain.port.out.UserRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService implements AdminUseCase {

    private final CommunitySourceRepository sourceRepository;
    private final PipelineConfigRepository configRepository;
    private final UserRepository userRepository;
    private final WeeklyExecutionRepository executionRepository;

    public AdminService(CommunitySourceRepository sourceRepository,
                        PipelineConfigRepository configRepository,
                        UserRepository userRepository,
                        WeeklyExecutionRepository executionRepository) {
        this.sourceRepository = sourceRepository;
        this.configRepository = configRepository;
        this.userRepository = userRepository;
        this.executionRepository = executionRepository;
    }

    @Override
    public List<SourceDto> getSources() {
        return sourceRepository.findAll().stream()
                .filter(CommunitySource::isActive)
                .map(this::mapToSourceDto)
                .collect(Collectors.toList());
    }

    @Override
    public SourceDto createSource(CreateSourceRequest request) {
        CommunitySource source = new CommunitySource();
        source.setName(request.name());
        source.setType(CommunitySource.SourceType.valueOf(request.type()));
        source.setApiUrl(request.apiUrl());
        source.setApiKeyEncrypted(request.apiKey()); // In real impl, encrypt with EncryptionService
        source.setActive(true);

        CommunitySource saved = sourceRepository.save(source);
        return mapToSourceDto(saved);
    }

    @Override
    public SourceDto updateSource(String id, UpdateSourceRequest request) {
        CommunitySource source = sourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Source not found"));

        if (request.name() != null) source.setName(request.name());
        if (request.apiUrl() != null) source.setApiUrl(request.apiUrl());
        if (request.apiKey() != null) source.setApiKeyEncrypted(request.apiKey());
        if (request.active() != null) source.setActive(request.active());

        CommunitySource saved = sourceRepository.save(source);
        return mapToSourceDto(saved);
    }

    @Override
    public void deleteSource(String id) {
        CommunitySource source = sourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Source not found"));
        source.setActive(false);
        sourceRepository.save(source);
    }

    @Override
    public ConfigDto getConfig() {
        PipelineConfig config = configRepository.findSingleton()
                .orElseGet(() -> {
                    PipelineConfig newConfig = new PipelineConfig();
                    return configRepository.save(newConfig);
                });
        return mapToConfigDto(config);
    }

    @Override
    public ConfigDto updateConfig(UpdateConfigRequest request) {
        PipelineConfig config = configRepository.findSingleton()
                .orElseGet(() -> new PipelineConfig());

        if (request.llmProvider() != null) config.setLlmProvider(request.llmProvider());
        if (request.llmModel() != null) config.setLlmModel(request.llmModel());
        if (request.newsletterPrompt() != null) config.setNewsletterPrompt(request.newsletterPrompt());
        if (request.linkedinPrompt() != null) config.setLinkedInPrompt(request.linkedinPrompt());
        if (request.twitterPrompt() != null) config.setTwitterPrompt(request.twitterPrompt());
        if (request.maxItemsPerChannel() != null) config.setMaxItemsPerChannel(request.maxItemsPerChannel());
        if (request.scheduleCron() != null) config.setScheduleCron(request.scheduleCron());

        PipelineConfig saved = configRepository.save(config);
        return mapToConfigDto(saved);
    }

    @Override
    public List<UserDto> getUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto createUser(CreateUserRequest request) {
        // Implementation needed
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public UserDto updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.role() != null) user.setRole(User.Role.valueOf(request.role()));
        if (request.active() != null) user.setActive(request.active());

        User saved = userRepository.save(user);
        return mapToUserDto(saved);
    }

    @Override
    public List<ExecutionSummaryDto> getExecutions() {
        return executionRepository.findAll().stream()
                .map(this::mapToExecutionDto)
                .collect(Collectors.toList());
    }

    @Override
    public ExecutionSummaryDto getExecutionDetail(String id) {
        WeeklyExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution not found"));
        return mapToExecutionDto(execution);
    }

    @Override
    public void triggerExecution(String triggeredBy) {
        // Implementation: trigger pipeline manually
        throw new RuntimeException("Not implemented yet");
    }

    private SourceDto mapToSourceDto(CommunitySource source) {
        return new SourceDto(
                source.getId(),
                source.getName(),
                source.getType() != null ? source.getType().name() : null,
                source.isActive()
        );
    }

    private ConfigDto mapToConfigDto(PipelineConfig config) {
        return new ConfigDto(
                config.getLlmProvider(),
                config.getLlmModel(),
                config.getNewsletterPrompt(),
                config.getLinkedInPrompt(),
                config.getTwitterPrompt(),
                config.getMaxItemsPerChannel(),
                config.getScheduleCron()
        );
    }

    private UserDto mapToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole() != null ? user.getRole().name() : null,
                user.isActive()
        );
    }

    private ExecutionSummaryDto mapToExecutionDto(WeeklyExecution execution) {
        return new ExecutionSummaryDto(
                execution.getId(),
                execution.getWeekStart() != null ? execution.getWeekStart().toString() : null,
                execution.getWeekEnd() != null ? execution.getWeekEnd().toString() : null,
                execution.getStatus() != null ? execution.getStatus().name() : null,
                execution.getStartedAt() != null ? execution.getStartedAt().toString() : null,
                execution.getCompletedAt() != null ? execution.getCompletedAt().toString() : null
        );
    }
}
