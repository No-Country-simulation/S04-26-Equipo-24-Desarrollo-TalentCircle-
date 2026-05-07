package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.AdminUseCase;
import com.talentcircle.domain.port.in.AdminUseCase.ExecutionSummaryDto;
import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for pipeline execution management.
 * RF-32: Ejecución Manual del Pipeline
 * RF-33: Historial de Ejecuciones
 */
@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {

    private final AdminUseCase adminUseCase;
    private final PipelineOrchestratorUseCase pipelineOrchestrator;

    public ExecutionController(AdminUseCase adminUseCase,
                               PipelineOrchestratorUseCase pipelineOrchestrator) {
        this.adminUseCase = adminUseCase;
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExecutionSummaryDto>> listExecutions() {
        return ResponseEntity.ok(adminUseCase.getExecutions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExecutionSummaryDto> getExecutionDetail(@PathVariable String id) {
        return ResponseEntity.ok(adminUseCase.getExecutionDetail(id));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerExecution(Authentication authentication) {
        String triggeredBy = authentication != null ? authentication.getName() : "MANUAL";
        String executionId = pipelineOrchestrator.runWeeklyPipeline(triggeredBy);
        return ResponseEntity.accepted().body(Map.of("executionId", executionId));
    }
}
