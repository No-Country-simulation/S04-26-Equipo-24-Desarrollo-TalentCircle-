package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.Draft;
import java.util.List;
import java.util.Optional;

public interface DraftRepository {
    Draft save(Draft draft);
    Optional<Draft> findById(String id);
    List<Draft> findByExecutionId(String executionId);
    List<Draft> findByFilters(String channel, String status, String weekStart, String weekEnd);
    List<Draft> findByStatus(com.talentcircle.domain.model.Draft.DraftStatus status);
}
