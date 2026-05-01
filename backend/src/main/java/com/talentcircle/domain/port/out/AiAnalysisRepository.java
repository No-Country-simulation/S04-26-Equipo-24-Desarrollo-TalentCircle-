package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.AiAnalysis;
import java.util.Optional;

public interface AiAnalysisRepository {
    AiAnalysis save(AiAnalysis analysis);
    Optional<AiAnalysis> findById(String id);
    Optional<AiAnalysis> findByExecutionId(String executionId);
}
