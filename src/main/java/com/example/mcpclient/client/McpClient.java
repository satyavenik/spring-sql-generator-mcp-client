package com.example.mcpclient.client;

import com.example.mcpclient.model.McpRequest;
import com.example.mcpclient.model.McpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class McpClient {

    private final WebClient webClient;

    public McpClient(@Value("${mcp.server.url}") String mcpServerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(mcpServerUrl)
                .build();
    }

    /**
     * Fetches context from MCP server based on the prompt
     * 
     * @param prompt The user's prompt
     * @return Context information from MCP server
     */
    public String fetchContext(String prompt) {
        log.debug("Fetching context from MCP server for prompt: {}", prompt);
        
        try {
            // Build MCP request to fetch resources or call tools
            McpRequest request = McpRequest.builder()
                    .jsonrpc("2.0")
                    .method("resources/list")
                    .params(Map.of("prompt", prompt))
                    .id(UUID.randomUUID().toString())
                    .build();

            // Call MCP server
            McpResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(McpResponse.class)
                    .block();

            if (response != null && response.getError() == null) {
                log.debug("Successfully fetched context from MCP server");
                return response.getResult().toString();
            } else if (response != null && response.getError() != null) {
                log.error("MCP server returned error: {}", response.getError().getMessage());
                return "Error fetching context: " + response.getError().getMessage();
            }
            
            return "No context available";
        } catch (Exception e) {
            log.error("Error calling MCP server", e);
            return "Error fetching context from MCP server: " + e.getMessage();
        }
    }

    /**
     * Calls a tool on the MCP server
     * 
     * @param toolName Name of the tool to call
     * @param arguments Tool arguments
     * @return Tool execution result
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        log.debug("Calling MCP tool: {} with arguments: {}", toolName, arguments);
        
        try {
            McpRequest request = McpRequest.builder()
                    .jsonrpc("2.0")
                    .method("tools/call")
                    .params(Map.of(
                            "name", toolName,
                            "arguments", arguments
                    ))
                    .id(UUID.randomUUID().toString())
                    .build();

            McpResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(McpResponse.class)
                    .block();

            if (response != null && response.getError() == null) {
                log.debug("Successfully called MCP tool: {}", toolName);
                return response.getResult().toString();
            } else if (response != null && response.getError() != null) {
                log.error("MCP tool call failed: {}", response.getError().getMessage());
                return "Error calling tool: " + response.getError().getMessage();
            }
            
            return "Tool call returned no result";
        } catch (Exception e) {
            log.error("Error calling MCP tool", e);
            return "Error calling MCP tool: " + e.getMessage();
        }
    }
}
