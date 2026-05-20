package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DraftRepository extends JpaRepository<Draft, String> {
    List<Draft> findByExecutionId(String executionId);
    List<Draft> findByStatus(com.talentcircle.domain.model.Draft.DraftStatus status);
    List<Draft> findByStatusAndCreatedAtBetween(Draft.DraftStatus status, LocalDateTime start, LocalDateTime end);
}
