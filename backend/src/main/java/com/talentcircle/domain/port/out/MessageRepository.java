package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    boolean existsByDiscordId(String discordId);
}
