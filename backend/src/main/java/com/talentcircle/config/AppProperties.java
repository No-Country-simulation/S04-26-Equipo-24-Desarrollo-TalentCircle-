package com.talentcircle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Typed configuration properties for the application.
 * Centralizes all app.* properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Llm llm = new Llm();
    private LinkedIn linkedin = new LinkedIn();
    private Cors cors = new Cors();
    private Pipeline pipeline = new Pipeline();

    public static class Jwt {
        private String secret;
        private long accessTokenExpiration = 28800000L;
        private long refreshTokenExpiration = 604800000L;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTokenExpiration() { return accessTokenExpiration; }
        public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
        public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
        public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
    }

    public static class Llm {
        private String provider = "openai";
        private OpenAi openai = new OpenAi();
        private Anthropic anthropic = new Anthropic();

        public static class OpenAi {
            private String apiKey;
            private String model = "gpt-4-turbo";
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
        }

        public static class Anthropic {
            private String apiKey;
            private String model = "claude-3-sonnet-20240229";
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
        }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public OpenAi getOpenai() { return openai; }
        public void setOpenai(OpenAi openai) { this.openai = openai; }
        public Anthropic getAnthropic() { return anthropic; }
        public void setAnthropic(Anthropic anthropic) { this.anthropic = anthropic; }
    }

    public static class LinkedIn {
        private String accessToken;
        private String personId;
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getPersonId() { return personId; }
        public void setPersonId(String personId) { this.personId = personId; }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Pipeline {
        private String scheduleCron = "0 0 18 * * FRI";
        public String getScheduleCron() { return scheduleCron; }
        public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    public LinkedIn getLinkedin() { return linkedin; }
    public void setLinkedin(LinkedIn linkedin) { this.linkedin = linkedin; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Pipeline getPipeline() { return pipeline; }
    public void setPipeline(Pipeline pipeline) { this.pipeline = pipeline; }
}
