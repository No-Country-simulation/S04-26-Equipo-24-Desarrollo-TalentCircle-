package com.talentcircle.adapter.in.web;

import com.talentcircle.application.service.DiscordMessageService;
import com.talentcircle.application.service.DiscordMessageService.CollectResult;
import com.talentcircle.common.dto.ApiResponse;
import com.talentcircle.common.exception.ForbiddenException;
import com.talentcircle.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for manually triggering Discord message collection.
 * Automatic collection runs every 30 minutes via {@link DiscordMessageService#scheduledCollect()}.
 *
 * <p>Response codes:
 * <ul>
 *   <li>201 Created  — new messages were saved</li>
 *   <li>200 OK       — collection ran but no new messages (all already stored)</li>
 *   <li>403 Forbidden — bot token invalid or missing permissions</li>
 *   <li>404 Not Found — guild or channel not found</li>
 *   <li>500 Internal Server Error — unexpected failure</li>
 * </ul>
 */
@RestController
@RequestMapping("/discord")
public class DiscordMessageController {

    private static final Logger log = LoggerFactory.getLogger(DiscordMessageController.class);

    private final DiscordMessageService discordMessageService;

    public DiscordMessageController(DiscordMessageService discordMessageService) {
        this.discordMessageService = discordMessageService;
    }

    /**
     * Manually triggers a Discord message collection.
     * Returns 201 if new messages were saved, 200 if nothing new was found.
     */
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<CollectResult>> collectMessages() {
        log.info("Manual Discord collection triggered via REST");
        try {
            CollectResult result = discordMessageService.collectMessages();

            String message = String.format(
                    "Collection completed — %d message(s) saved, %d skipped",
                    result.saved(), result.skipped());

            // 201 when at least one new message was persisted, 200 otherwise
            HttpStatus status = result.saved() > 0 ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(true, result, message, null));

        } catch (ForbiddenException ex) {
            log.warn("Discord collection forbidden: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ex.getMessage()));

        } catch (ResourceNotFoundException ex) {
            log.warn("Discord collection resource not found: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));

        } catch (Exception ex) {
            log.error("Discord collection failed unexpectedly: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Discord collection failed: " + ex.getMessage()));
        }
    }
}
