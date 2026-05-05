package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, String> {
    List<Publication> findByDraftId(String draftId);
}
