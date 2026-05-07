package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.DraftSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DraftSourceRepository extends JpaRepository<DraftSource, String> {
    List<DraftSource> findByDraftId(String draftId);
}
