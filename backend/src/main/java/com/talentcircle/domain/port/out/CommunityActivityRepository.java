package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.CommunityActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, String> {
    List<CommunityActivity> findByExecutionId(String executionId);
}
