package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.DraftVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DraftVersionRepository extends JpaRepository<DraftVersion, String> {
    List<DraftVersion> findByDraftIdOrderByVersionNumberAsc(String draftId);
    int countByDraftId(String draftId);
}
