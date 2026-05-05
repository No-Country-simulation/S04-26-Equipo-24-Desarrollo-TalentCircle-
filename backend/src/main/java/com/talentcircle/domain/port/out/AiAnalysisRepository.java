package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, String> {
    Optional<AiAnalysis> findByExecutionId(String executionId);
}
