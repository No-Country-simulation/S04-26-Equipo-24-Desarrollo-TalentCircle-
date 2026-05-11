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
 * Adapter for Slack community API.
 * Fetches messages, threads and shared resources from Slack workspaces.
 * In production, uses Slack Web API with OAuth bot token.
 */
@Component("slackClientAdapter")
public class SlackClientAdapter implements CommunityClientPort {

    private static final Logger log = LoggerFactory.getLogger(SlackClientAdapter.class);

    @Override
    public List<CommunityActivity> fetchTopPosts(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} posts from Slack: {}", limit, apiUrl);
        // Production: POST https://slack.com/api/conversations.history
        // Filter by reaction count using reactions.list, sort descending
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.POST, limit);
    }

    @Override
    public List<CommunityActivity> fetchTopQuestions(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} questions from Slack: {}", limit, apiUrl);
        // Production: GET conversations.history + replies count per thread
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.QUESTION, limit);
    }

    @Override
    public List<CommunityActivity> fetchTopResources(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} resources from Slack: {}", limit, apiUrl);
        // Production: GET files.list with type=all, sort by shares
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.RESOURCE, limit);
    }

    @Override
    public boolean validateConnection(String apiUrl, String apiKey) {
        log.info("Validating Slack connection: {}", apiUrl);
        // Production: POST https://slack.com/api/auth.test with bot token
        return apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    private List<CommunityActivity> buildSampleActivities(String apiUrl, CommunityActivity.ActivityType type,
                                                           int limit) {
        List<CommunityActivity> activities = new ArrayList<>();
        for (int i = 1; i <= Math.min(limit, 3); i++) {
            CommunityActivity activity = new CommunityActivity();
            activity.setType(type);
            activity.setTitle("Slack " + type.name() + " #" + i);
            activity.setContent("Community content from Slack workspace - item " + i);
            activity.setAuthor("slack_user_" + i);
            activity.setPublishedAt(LocalDateTime.now().minusDays(i));
            activity.setSourceUrl(apiUrl + "/archives/C0123456/p" + UUID.randomUUID().toString().replace("-", ""));
            activity.setReactionCount(18 - i);
            activity.setResponseCount(9 - i);
            activity.setShareCount(4 - i);
            activities.add(activity);
        }
        return activities;
    }
}
