package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.WeeklyExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WeeklyExecutionRepository extends JpaRepository<WeeklyExecution, String> {
    Optional<WeeklyExecution> findByWeekStart(java.time.LocalDate weekStart);
}
