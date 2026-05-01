package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.CommunityActivity;
import java.util.List;

public interface CommunityActivityRepository {
    CommunityActivity save(CommunityActivity activity);
    List<CommunityActivity> findByExecutionId(String executionId);
    void delete(CommunityActivity activity);
}
