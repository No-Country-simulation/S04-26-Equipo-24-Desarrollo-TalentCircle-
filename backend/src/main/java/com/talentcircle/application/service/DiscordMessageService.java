package com.talentcircle.application.service;

import com.talentcircle.domain.model.MessageEntity;
import com.talentcircle.domain.port.out.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;

@Service
public class DiscordMessageService {

    private final MessageRepository messageRepository;

    @Value("${app.discord.bot-token}")
    private String botToken;

    @Value("${app.discord.guild-id}")
    private String guildId;

    public DiscordMessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void collectMessages() throws Exception {
        JDA jda = JDABuilder.createLight(botToken,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady();

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            System.out.println("⚠️ No se encontró el servidor con ID " + guildId);
            return;
        }

        // Tomar el canal "general" como prueba
        TextChannel channel = guild.getTextChannelsByName("backend-java", true).get(0);

        List<Message> messages = channel.getHistory().retrievePast(20).complete();

        for (Message msg : messages) {
            if (!msg.getAuthor().isBot()) {
                System.out.println("📩 " + msg.getAuthor().getName() + ": " + msg.getContentDisplay());

                // Guardar en BD
                MessageEntity entity = new MessageEntity();
                entity.setDiscordId(msg.getId());
                entity.setAuthor(msg.getAuthor().getName());
                entity.setContent(msg.getContentDisplay());
                entity.setPublishedAt(msg.getTimeCreated().toInstant()
                        .atZone(ZoneOffset.UTC).toLocalDateTime());

                messageRepository.save(entity);
            }
        }

        jda.shutdown();
    }
}

