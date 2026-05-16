package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.port.out.LlmClientPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/test/openai")
@Tag(name = "OpenAI Test", description = "Endpoints de prueba para integración con OpenAI")
public class OpenAiTestController {

    private final LlmClientPort llmClient;
    private final String apiKey;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public OpenAiTestController(LlmClientPort llmClient,
                                @Value("${app.llm.openai.api-key:}") String apiKey) {
        this.llmClient = llmClient;
        this.apiKey = apiKey;
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming de respuesta OpenAI (SSE)",
               description = "Envía un prompt a OpenAI y recibe la respuesta token por token via Server-Sent Events")
    public SseEmitter streamResponse(
            @RequestParam(defaultValue = "Hola, cuéntame sobre tecnología en español.") String prompt) {
        SseEmitter emitter = new SseEmitter(120_000L);

        executor.execute(() -> {
            try {
                String body = String.format("""
                        {
                          "model": "gpt-4o-mini",
                          "messages": [
                            {"role": "system", "content": "Eres un asistente útil que responde en español."},
                            {"role": "user", "content": "%s"}
                          ],
                          "stream": true,
                          "max_tokens": 500
                        }
                        """, prompt.replace("\"", "\\\""));

                URI uri = URI.create("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                conn.getOutputStream().write(body.getBytes());

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(data, MediaType.APPLICATION_JSON));
                    }
                }
                reader.close();
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Error: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
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
