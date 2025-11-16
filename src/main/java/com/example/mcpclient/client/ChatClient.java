package com.example.mcpclient.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom ChatClient wrapper for calling OpenAI API
 */
@Slf4j
@Component
public class ChatClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public ChatClient(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Send a prompt to the LLM and get a response
     * 
     * @param prompt The prompt to send
     * @return The LLM response content
     */
    public String call(String prompt) {
        log.debug("Calling LLM with prompt: {}", prompt);
        
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            // Call OpenAI API
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.size() > 0) {
                    String content = choices.get(0).get("message").get("content").asText();
                    log.debug("LLM response: {}", content);
                    return content;
                }
            }
            
            return "No response from LLM";
        } catch (Exception e) {
            log.error("Error calling LLM", e);
            return "Error calling LLM: " + e.getMessage();
        }
    }
}
