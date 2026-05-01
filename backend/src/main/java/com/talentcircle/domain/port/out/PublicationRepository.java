package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.Publication;
import java.util.Optional;

public interface PublicationRepository {
    Publication save(Publication publication);
    Optional<Publication> findByDraftId(String draftId);
    void delete(Publication publication);
}
