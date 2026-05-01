package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.PipelineConfig;
import java.util.Optional;

public interface PipelineConfigRepository {
    PipelineConfig save(PipelineConfig config);
    Optional<PipelineConfig> findSingleton();
}
