package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.PipelineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineConfigRepository extends JpaRepository<PipelineConfig, String> {
    List<PipelineConfig> findAll();
    default Optional<PipelineConfig> findSingleton() {
        List<PipelineConfig> all = findAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }
}
