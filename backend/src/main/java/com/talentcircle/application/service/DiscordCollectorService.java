package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.CommunitySource;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio que conecta con la API de Discord via JDA,
 * lee los mensajes de la semana (lunes a viernes) de todos los canales
 * de texto del servidor, filtra por engagement y los persiste.
 */
@Service
public class DiscordCollectorService {

    private static final Logger log = LoggerFactory.getLogger(DiscordCollectorService.class);

    /** Mínimo de reacciones + respuestas para considerar un mensaje relevante */
    private static final int MIN_ENGAGEMENT = 0;

    /** Máximo de mensajes a guardar por canal por ejecución */
    private static final int MAX_MESSAGES_PER_CHANNEL = 1;

    /** Cuántos mensajes históricos pedir a Discord por canal */
    private static final int HISTORY_LIMIT = 100;

    @Value("${app.discord.bot-token:}")
    private String botToken;

    @Value("${app.discord.guild-id:}")
    private String guildId;

    private final CommunityActivityRepository activityRepository;

    public DiscordCollectorService(CommunityActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /**
     * Recolecta las mejores conversaciones de Discord de la semana actual (lunes-viernes).
     *
     * @param source       La fuente de comunidad configurada (contiene el bot token si se sobreescribe)
     * @param execution    La ejecución semanal a la que se asociarán las actividades
     * @return             Lista de actividades guardadas
     */
    public List<CommunityActivity> collectWeeklyActivities(
            CommunitySource source,
            com.talentcircle.domain.model.WeeklyExecution execution) {

        String token = resolveToken(source);

        if (token == null || token.isBlank()) {
            log.warn("Discord bot token no configurado para la fuente '{}'. Saltando.", source.getName());
            return List.of();
        }

        String serverId = resolveGuildId(source);
        if (serverId == null || serverId.isBlank()) {
            log.warn("Discord guild ID no configurado. Saltando.");
            return List.of();
        }

        // Rango lunes-viernes de la semana actual
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to   = friday.atTime(23, 59, 59);

        log.info("Recolectando actividad Discord del {} al {} para fuente '{}'",
                monday, friday, source.getName());

        JDA jda = null;
        List<CommunityActivity> saved = new ArrayList<>();

        try {
            jda = JDABuilder.createLight(token,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT)
                    .build()
                    .awaitReady();

            Guild guild = jda.getGuildById(serverId);
            if (guild == null) {
                log.error("No se encontró el servidor de Discord con ID '{}'", serverId);
                return List.of();
            }

            List<TextChannel> channels = guild.getTextChannels();
            log.info("Procesando {} canales de texto en '{}'", channels.size(), guild.getName());

            for (TextChannel channel : channels) {
                if (!channel.canTalk()) {
                    log.debug("Sin permisos de lectura en canal '{}', saltando.", channel.getName());
                    continue;
                }

                List<CommunityActivity> channelActivities =
                        processChannel(channel, source, execution, from, to);
                saved.addAll(channelActivities);
            }

            log.info("Recolección Discord completada: {} actividades guardadas.", saved.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupción al conectar con Discord", e);
        } catch (Exception e) {
            log.error("Error al recolectar actividad de Discord: {}", e.getMessage(), e);
        } finally {
            if (jda != null) {
                jda.shutdown();
            }
        }

        return saved;
    }

    // ── Procesamiento por canal ───────────────────────────────────────────────

    private List<CommunityActivity> processChannel(
            TextChannel channel,
            CommunitySource source,
            com.talentcircle.domain.model.WeeklyExecution execution,
            LocalDateTime from,
            LocalDateTime to) {

        List<CommunityActivity> result = new ArrayList<>();

        try {
            // Obtener historial del canal
            List<Message> messages = channel.getHistory()
                    .retrievePast(HISTORY_LIMIT)
                    .complete();

            // Filtrar por rango de fechas y engagement mínimo
            List<Message> relevant = messages.stream()
                    .filter(m -> !m.getAuthor().isBot())
                    .filter(m -> {
                        LocalDateTime ts = m.getTimeCreated()
                                .toInstant()
                                .atZone(ZoneOffset.UTC)
                                .toLocalDateTime();
                        return !ts.isBefore(from) && !ts.isAfter(to);
                    })
                    .filter(m -> engagementScore(m) >= MIN_ENGAGEMENT)
                    .sorted(Comparator.comparingInt(this::engagementScore).reversed())
                    .limit(MAX_MESSAGES_PER_CHANNEL)
                    .toList();

            log.debug("Canal '{}': {} mensajes relevantes de {} totales",
                    channel.getName(), relevant.size(), messages.size());

            for (Message message : relevant) {
                // Deduplicación — no guardar si ya existe
                if (activityRepository.existsByDiscordMessageId(message.getId())) {
                    log.debug("Mensaje {} ya existe, saltando.", message.getId());
                    continue;
                }

                CommunityActivity activity = buildActivity(message, channel, source, execution);
                activityRepository.save(activity);
                result.add(activity);
            }

        } catch (Exception e) {
            log.warn("Error procesando canal '{}': {}", channel.getName(), e.getMessage());
        }

        return result;
    }

    // ── Construcción de la entidad ────────────────────────────────────────────

    private CommunityActivity buildActivity(
            Message message,
            TextChannel channel,
            CommunitySource source,
            com.talentcircle.domain.model.WeeklyExecution execution) {

        CommunityActivity activity = new CommunityActivity();
        activity.setExecution(execution);
        activity.setSourceId(source.getId());
        activity.setDiscordMessageId(message.getId());
        activity.setDiscordChannelId(channel.getId());

        // Título: primeras 120 chars del mensaje o el nombre del canal
        String content = message.getContentDisplay();
        String title = content.length() > 120
                ? content.substring(0, 117) + "..."
                : content;
        activity.setTitle(title.isBlank() ? "#" + channel.getName() : title);
        activity.setContent(content);

        activity.setAuthor(message.getAuthor().getName());
        activity.setPublishedAt(message.getTimeCreated()
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime());

        // URL directa al mensaje en Discord
        activity.setSourceUrl(String.format(
                "https://discord.com/channels/%s/%s/%s",
                channel.getGuild().getId(),
                channel.getId(),
                message.getId()));

        // Engagement
        int reactions = message.getReactions().stream()
                .mapToInt(r -> r.getCount())
                .sum();
        activity.setReactionCount(reactions);
        activity.setResponseCount(0); // Discord no expone replies directamente en este endpoint
        activity.setShareCount(0);

        // Clasificar tipo según contenido
        activity.setType(classifyType(content));

        return activity;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int engagementScore(Message message) {
        int reactions = message.getReactions().stream()
                .mapToInt(r -> r.getCount())
                .sum();
        // Penalizar mensajes muy cortos (menos de 50 chars)
        int lengthBonus = message.getContentDisplay().length() > 50 ? 1 : 0;
        return reactions + lengthBonus;
    }

    private CommunityActivity.ActivityType classifyType(String content) {
        if (content == null) return CommunityActivity.ActivityType.POST;
        String lower = content.toLowerCase();
        if (lower.contains("?") || lower.startsWith("cómo") || lower.startsWith("como")
                || lower.startsWith("qué") || lower.startsWith("que")) {
            return CommunityActivity.ActivityType.QUESTION;
        }
        if (lower.contains("http") || lower.contains("recurso") || lower.contains("link")
                || lower.contains("artículo") || lower.contains("articulo")) {
            return CommunityActivity.ActivityType.RESOURCE;
        }
        return CommunityActivity.ActivityType.POST;
    }

    private String resolveToken(CommunitySource source) {
        // Si la fuente tiene su propio token cifrado, usarlo; si no, usar el global
        if (source.getApiKeyEncrypted() != null && !source.getApiKeyEncrypted().isBlank()) {
            return source.getApiKeyEncrypted(); // En producción: desencriptar con EncryptionService
        }
        return botToken;
    }

    private String resolveGuildId(CommunitySource source) {
        // El apiUrl de la fuente puede contener el guild ID
        if (source.getApiUrl() != null && !source.getApiUrl().isBlank()) {
            return source.getApiUrl();
        }
        return guildId;
    }
}
