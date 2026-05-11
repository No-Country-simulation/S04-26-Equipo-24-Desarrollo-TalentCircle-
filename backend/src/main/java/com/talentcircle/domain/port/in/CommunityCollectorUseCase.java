package com.talentcircle.domain.port.in;

import java.util.List;

public interface CommunityCollectorUseCase {
    void collectActivity(String executionId, String sourceId);
    void collectFromAllActiveSources(String executionId);
    List<CommunityActivityDto> getActivitiesByExecution(String executionId);

    record CommunityActivityDto(
            String id,
            String title,
            String content,
            String type,
            Integer reactionCount,
            Integer responseCount,
            Integer shareCount,
            String author,
            String sourceUrl
    ) {}
}
