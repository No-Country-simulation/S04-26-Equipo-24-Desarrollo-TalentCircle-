package com.talentcircle.application.service;

import com.talentcircle.common.exception.ForbiddenException;
import com.talentcircle.common.exception.ResourceNotFoundException;
import com.talentcircle.domain.model.MessageEntity;
import com.talentcircle.domain.port.out.MessageRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Service responsible for collecting Discord messages and persisting them.
 * Runs automatically every 30 minutes via {@link #scheduledCollect()}.
 * Can also be triggered manually via the REST endpoint.
 */
@Service
@EnableScheduling
public class DiscordMessageService {

    private static final Logger log = LoggerFactory.getLogger(DiscordMessageService.class);

    private final MessageRepository messageRepository;

    @Value("${app.discord.bot-token:}")
    private String botToken;

    @Value("${app.discord.guild-id:}")
    private String guildId;

    @Value("${app.discord.channel-name:backend-java}")
    private String channelName;

    public DiscordMessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    // ── Scheduled collection ──────────────────────────────────────────────────

    /**
     * Automatically collects Discord messages every 30 minutes.
     * Runs 30 minutes after application startup, then every 30 minutes.
     */
    @Scheduled(fixedDelayString = "${app.discord.collect-interval-ms:1800000}",
               initialDelayString = "${app.discord.initial-delay-ms:1800000}")
    public void scheduledCollect() {
        log.info("Scheduled Discord collection triggered");
        try {
            CollectResult result = collectMessages();
            log.info("Scheduled Discord collection completed — saved={}, skipped={}",
                    result.saved(), result.skipped());
        } catch (Exception e) {
            log.error("Scheduled Discord collection failed: {}", e.getMessage(), e);
        }
    }

    // ── Core collection logic ─────────────────────────────────────────────────

    /**
     * Connects to Discord, retrieves the last 20 messages from the configured
     * channel, persists new ones (deduplicating by discordId), and returns a
     * summary of the operation.
     *
     * @return {@link CollectResult} with counts of saved and skipped messages
     * @throws ForbiddenException        if the bot token is invalid or lacks permissions (403)
     * @throws ResourceNotFoundException if the guild or channel cannot be found (404)
     * @throws RuntimeException          for any other unexpected error (500)
     */
    public CollectResult collectMessages() {
        JDA jda = null;
        try {
            jda = buildJda();
        } catch (Exception e) {
            // JDA throws on invalid token / missing intents → treat as 403
            String msg = e.getMessage() != null ? e.getMessage() : "Discord authentication failed";
            if (msg.contains("401") || msg.contains("token") || msg.contains("Unauthorized")) {
                throw new ForbiddenException("Discord bot token is invalid or lacks required permissions");
            }
            throw new RuntimeException("Failed to connect to Discord: " + msg, e);
        }

        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                throw new ResourceNotFoundException(
                        "Discord guild not found with id: '" + guildId + "'");
            }

            List<TextChannel> channels = guild.getTextChannelsByName(channelName, true);
            if (channels.isEmpty()) {
                throw new ResourceNotFoundException(
                        "Discord channel not found with name: '" + channelName + "'");
            }

            TextChannel channel = channels.get(0);
            List<Message> messages = channel.getHistory().retrievePast(20).complete();

            int saved = 0;
            int skipped = 0;

            for (Message msg : messages) {
                if (msg.getAuthor().isBot()) {
                    skipped++;
                    continue;
                }

                // Deduplicate: skip messages already in the database
                if (messageRepository.existsByDiscordId(msg.getId())) {
                    skipped++;
                    continue;
                }

                MessageEntity entity = new MessageEntity();
                entity.setDiscordId(msg.getId());
                entity.setAuthor(msg.getAuthor().getName());
                entity.setContent(msg.getContentDisplay());
                entity.setPublishedAt(msg.getTimeCreated().toInstant()
                        .atZone(ZoneOffset.UTC).toLocalDateTime());

                messageRepository.save(entity);
                saved++;
                log.debug("Saved message discordId={} author={}", msg.getId(), msg.getAuthor().getName());
            }

            return new CollectResult(saved, skipped);

        } finally {
            jda.shutdown();
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    JDA buildJda() throws Exception {
        return JDABuilder.createLight(botToken,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady();
    }

    // ── Result DTO ────────────────────────────────────────────────────────────

    /**
     * Summary of a collection run.
     *
     * @param saved   number of new messages persisted
     * @param skipped number of messages skipped (bots or already stored)
     */
    public record CollectResult(int saved, int skipped) {}
}
