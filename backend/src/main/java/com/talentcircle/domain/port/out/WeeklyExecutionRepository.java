package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.WeeklyExecution;
import java.util.Optional;

public interface WeeklyExecutionRepository {
    WeeklyExecution save(WeeklyExecution execution);
    Optional<WeeklyExecution> findById(String id);
    Optional<WeeklyExecution> findByWeekStart(java.time.LocalDate weekStart);
    java.util.List<WeeklyExecution> findAll();
}
