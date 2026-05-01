package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.CommunitySource;
import java.util.List;
import java.util.Optional;

public interface CommunitySourceRepository {
    CommunitySource save(CommunitySource source);
    Optional<CommunitySource> findById(String id);
    List<CommunitySource> findAllActive();
    void delete(CommunitySource source);
}
