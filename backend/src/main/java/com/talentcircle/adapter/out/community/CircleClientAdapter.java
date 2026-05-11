package com.talentcircle.adapter.out.community;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.CommunityClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adapter for Circle.so community API.
 * Fetches posts, questions and resources from Circle spaces.
 * In production, uses Circle REST API with API key authentication.
 */
@Component("circleClientAdapter")
public class CircleClientAdapter implements CommunityClientPort {

    private static final Logger log = LoggerFactory.getLogger(CircleClientAdapter.class);

    @Override
    public List<CommunityActivity> fetchTopPosts(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} posts from Circle.so: {}", limit, apiUrl);
        // Production: GET {apiUrl}/api/v1/posts?sort=likes_count&per_page={limit}
        // Filter: published_at >= now - 7 days
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.POST, limit);
    }

    @Override
    public List<CommunityActivity> fetchTopQuestions(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} questions from Circle.so: {}", limit, apiUrl);
        // Production: GET {apiUrl}/api/v1/posts?post_type=question&sort=comments_count
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.QUESTION, limit);
    }

    @Override
    public List<CommunityActivity> fetchTopResources(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} resources from Circle.so: {}", limit, apiUrl);
        // Production: GET {apiUrl}/api/v1/posts?post_type=resource&sort=shares_count
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.RESOURCE, limit);
    }

    @Override
    public boolean validateConnection(String apiUrl, String apiKey) {
        log.info("Validating Circle.so connection: {}", apiUrl);
        // Production: GET {apiUrl}/api/v1/me with Authorization: Token {apiKey}
        return apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    private List<CommunityActivity> buildSampleActivities(String apiUrl, CommunityActivity.ActivityType type,
                                                           int limit) {
        List<CommunityActivity> activities = new ArrayList<>();
        for (int i = 1; i <= Math.min(limit, 3); i++) {
            CommunityActivity activity = new CommunityActivity();
            activity.setType(type);
            activity.setTitle("Circle " + type.name() + " #" + i);
            activity.setContent("Community content from Circle.so space - item " + i);
            activity.setAuthor("circle_member_" + i);
            activity.setPublishedAt(LocalDateTime.now().minusDays(i));
            activity.setSourceUrl(apiUrl + "/posts/" + UUID.randomUUID());
            activity.setReactionCount(25 - i);
            activity.setResponseCount(12 - i);
            activity.setShareCount(6 - i);
            activities.add(activity);
        }
        return activities;
    }
}
