package com.talentcircle.adapter.in.web;

import com.talentcircle.application.service.DiscordMessageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiscordMessageController {

    private final DiscordMessageService discordMessageService;

    public DiscordMessageController(DiscordMessageService discordMessageService) {
        this.discordMessageService = discordMessageService;
    }

    @PostMapping("/discord/collect")
    public String collectMessages() {
        try {
            discordMessageService.collectMessages();
            return "✅ Recolección completada. Revisa la consola y la base de datos.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error en la recolección: " + e.getMessage();
        }
    }
}
