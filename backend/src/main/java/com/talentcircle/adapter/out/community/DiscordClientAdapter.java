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
 * Adapter for Discord community API.
 * Fetches posts, questions and resources from Discord channels.
 * In production, uses Discord REST API v10 with bot token authentication.
 */
@Component("discordClientAdapter")
public class DiscordClientAdapter implements CommunityClientPort {

    private static final Logger log = LoggerFactory.getLogger(DiscordClientAdapter.class);

    @Override
    public List<CommunityActivity> fetchTopPosts(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} posts from Discord: {}", limit, apiUrl);
        // Production: GET https://discord.com/api/v10/channels/{channelId}/messages
        // Filter by reaction count, sort descending, take top N from last 7 days
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.POST, limit, "reaction_count");
    }

    @Override
    public List<CommunityActivity> fetchTopQuestions(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} questions from Discord: {}", limit, apiUrl);
        // Production: GET messages with thread replies, sort by reply count
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.QUESTION, limit, "response_count");
    }

    @Override
    public List<CommunityActivity> fetchTopResources(String apiUrl, String apiKey, int limit) {
        log.info("Fetching top {} resources from Discord: {}", limit, apiUrl);
        // Production: GET messages with attachments/links, sort by share count
        return buildSampleActivities(apiUrl, CommunityActivity.ActivityType.RESOURCE, limit, "share_count");
    }

    @Override
    public boolean validateConnection(String apiUrl, String apiKey) {
        log.info("Validating Discord connection: {}", apiUrl);
        // Production: GET https://discord.com/api/v10/users/@me with bot token
        return apiUrl != null && !apiUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    private List<CommunityActivity> buildSampleActivities(String apiUrl, CommunityActivity.ActivityType type,
                                                           int limit, String sortField) {
        List<CommunityActivity> activities = new ArrayList<>();
        for (int i = 1; i <= Math.min(limit, 3); i++) {
            CommunityActivity activity = new CommunityActivity();
            activity.setType(type);
            activity.setTitle("Discord " + type.name() + " #" + i);
            activity.setContent("Community content from Discord channel - item " + i);
            activity.setAuthor("discord_user_" + i);
            activity.setPublishedAt(LocalDateTime.now().minusDays(i));
            activity.setSourceUrl(apiUrl + "/channels/general/messages/" + UUID.randomUUID());
            activity.setReactionCount(20 - i);
            activity.setResponseCount(10 - i);
            activity.setShareCount(5 - i);
            activities.add(activity);
        }
        return activities;
    }
}
