package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.CommunityActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, String> {
    List<CommunityActivity> findByExecutionId(String executionId);
    boolean existsByDiscordMessageId(String discordMessageId);
    Optional<CommunityActivity> findByDiscordMessageId(String discordMessageId);
}
