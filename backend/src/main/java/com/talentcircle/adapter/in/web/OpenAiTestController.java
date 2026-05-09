package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test/openai")
@Tag(name = "OpenAI Test", description = "Endpoints de prueba para integración con OpenAI")
public class OpenAiTestController {

    private final LlmClientPort llmClient;

    public OpenAiTestController(LlmClientPort llmClient) {
        this.llmClient = llmClient;
    }

    @GetMapping("/ping")
    @Operation(summary = "Probar conexión con OpenAI", description = "Valida si la API key está configurada y funciona")
    public ResponseEntity<String> pingOpenAi(
            @RequestParam(value = "apiKey", required = false) String apiKey) {
        try {
            boolean valid = false;
            if (apiKey != null && !apiKey.isEmpty()) {
                valid = llmClient.validateConnection(apiKey);
            } else {
                // Usar la key configurada en el sistema
                CommunityActivity dummy = new CommunityActivity();
                dummy.setTitle("Test Activity");
                dummy.setContent("This is a test message about technology trends.");
                dummy.setType(CommunityActivity.ActivityType.POST);
                dummy.setAuthor("Test User");

                AiAnalysis analysis = llmClient.analyzeActivity(
                        List.of(dummy),
                        "Analyze this community activity and give a brief summary in Spanish (max 50 words)."
                );
                valid = analysis != null && analysis.getExecutiveSummary() != null;
            }

            if (valid) {
                return ResponseEntity.ok("OpenAI connection: SUCCESS");
            } else {
                return ResponseEntity.badRequest().body("OpenAI connection: FAILED - Invalid API key");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("OpenAI connection: FAILED - " + e.getMessage());
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "Enviar mensaje a OpenAI", description = "Prueba el chat completions endpoint")
    public ResponseEntity<String> chatWithOpenAi(@RequestBody ChatRequest request) {
        try {
            CommunityActivity dummy = new CommunityActivity();
            dummy.setTitle("Test Activity");
            dummy.setContent(request.message);
            dummy.setType(CommunityActivity.ActivityType.POST);
            dummy.setAuthor("User");

            AiAnalysis analysis = llmClient.analyzeActivity(
                    List.of(dummy),
                    request.prompt != null ? request.prompt : "Respond to this message in Spanish (max 100 words)."
            );

            return ResponseEntity.ok(analysis.getExecutiveSummary());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    @Operation(summary = "Generar borrador", description = "Prueba la generación de borradores para un canal")
    public ResponseEntity<String> generateDraft(@RequestBody GenerateRequest request) {
        try {
            String result = llmClient.generateDraft(
                    request.analysisJson != null ? request.analysisJson : "{\"summary\": \"Sample analysis\"}",
                    request.channel != null ? request.channel : "LINKEDIN",
                    request.prompt != null ? request.prompt :
                            "Generate a LinkedIn post in Spanish based on this analysis. Keep it engaging and under 300 characters."
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    public record ChatRequest(
            String message,
            String prompt
    ) {}

    public record GenerateRequest(
            String analysisJson,
            String channel,
            String prompt
    ) {}
}
