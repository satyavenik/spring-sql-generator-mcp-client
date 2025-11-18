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

    /**
     * Call LLM with function calling capabilities for tool selection
     *
     * @param prompt The prompt to send
     * @param functions Available functions/tools
     * @return The LLM response with potential function call
     */
    public String callWithFunctions(String prompt, List<Map<String, Object>> functions) {
        log.debug("Calling LLM with functions: {}", functions.size());

        try {
            // Build request body with function calling
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You must call one of the available functions. Do not provide text responses."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("functions", functions);
            requestBody.put("function_call", "auto");
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 200);

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
                    JsonNode message = choices.get(0).get("message");
                    JsonNode functionCall = message.get("function_call");

                    if (functionCall != null) {
                        // Return function call details as JSON
                        String functionName = functionCall.get("name").asText();
                        String arguments = functionCall.get("arguments").asText();

                        Map<String, Object> result = new HashMap<>();
                        result.put("tool", functionName);

                        // Parse arguments JSON string
                        try {
                            JsonNode argsNode = objectMapper.readTree(arguments);
                            Map<String, Object> argsMap = new HashMap<>();
                            argsNode.fields().forEachRemaining(entry -> {
                                argsMap.put(entry.getKey(), entry.getValue().asText());
                            });
                            result.put("arguments", argsMap);
                        } catch (Exception e) {
                            result.put("arguments", new HashMap<>());
                        }

                        return objectMapper.writeValueAsString(result);
                    } else {
                        // LLM returned text instead of function call - create fallback
                        log.warn("LLM returned text instead of function call, using fallback");
                        return createFallbackToolSelection(prompt, functions);
                    }
                }
            }

            return createFallbackToolSelection(prompt, functions);
        } catch (Exception e) {
            log.error("Error calling LLM with functions", e);
            return createFallbackToolSelection(prompt, functions);
        }
    }

    /**
     * Creates a fallback tool selection when LLM doesn't use function calling
     */
    private String createFallbackToolSelection(String prompt, List<Map<String, Object>> functions) {
        try {
            // Simple heuristic-based tool selection
            String lowerPrompt = prompt.toLowerCase();

            for (Map<String, Object> function : functions) {
                String name = (String) function.get("name");

                // Match based on keywords
                if (name.equals("get_template") && (lowerPrompt.contains("template") || lowerPrompt.contains("example"))) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("tool", "get_template");
                    result.put("arguments", Map.of("name", "default"));
                    return objectMapper.writeValueAsString(result);
                } else if (name.equals("get_templates") && lowerPrompt.contains("templates")) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("tool", "get_templates");
                    result.put("arguments", new HashMap<>());
                    return objectMapper.writeValueAsString(result);
                }
            }

            // Default to get_schema
            Map<String, Object> result = new HashMap<>();
            result.put("tool", "get_schema");
            result.put("arguments", new HashMap<>());
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.error("Error creating fallback tool selection", e);
            return "{\"tool\":\"get_schema\",\"arguments\":{}}";
        }
    }
}
