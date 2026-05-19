package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.port.out.DraftRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/newsletters")
public class NewsletterController {

    private final DraftRepository draftRepository;

    public NewsletterController(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    public record PublishedNewsletterDto(
            String id,
            String title,
            String date,
            String excerpt
    ) {}

    @GetMapping
    public ResponseEntity<List<PublishedNewsletterDto>> getPublishedNewsletters() {
        List<Draft> newsletters = draftRepository.findByChannelAndStatusOrderByCreatedAtDesc(
                Draft.Channel.NEWSLETTER, Draft.DraftStatus.PUBLISHED);

        List<PublishedNewsletterDto> dtos = newsletters.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    private PublishedNewsletterDto toDto(Draft draft) {
        String content = draft.getEditedContent() != null ? draft.getEditedContent() : draft.getContent();
        return new PublishedNewsletterDto(
                draft.getId(),
                extractTitle(content),
                draft.getCreatedAt() != null ? draft.getCreatedAt().toLocalDate().toString() : "",
                extractExcerpt(content)
        );
    }

    private String extractTitle(String content) {
        if (content == null || content.isBlank()) return "Untitled";
        String firstLine = content.split("\n")[0].trim();
        return firstLine.length() > 100 ? firstLine.substring(0, 100) + "..." : firstLine;
    }

    private String extractExcerpt(String content) {
        if (content == null || content.isBlank()) return "";
        String plain = content.replaceAll("#{1,6}\\s?", "").replaceAll("\\*\\*?", "").trim();
        return plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
    }
}
