package com.example.mcpclient.client;

import com.example.mcpclient.model.McpRequest;
import com.example.mcpclient.model.McpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
            // Step 1: List available resources
            McpRequest listRequest = McpRequest.builder()
                    .jsonrpc("2.0")
                    .method("resources/list")
                    .params(Map.of())
                    .id(UUID.randomUUID().toString())
                    .build();

            McpResponse listResponse = webClient.post()
                    .bodyValue(listRequest)
                    .retrieve()
                    .bodyToMono(McpResponse.class)
                    .block();

            if (listResponse == null || listResponse.getError() != null) {
                String errorMsg = listResponse != null ? listResponse.getError().getMessage() : "No response";
                log.error("MCP server returned error on resources/list: {}", errorMsg);
                return "Error fetching resource list: " + errorMsg;
            }

            log.debug("Resources list response: {}", listResponse.getResult());

            // Step 2: Read the actual content of each resource
            StringBuilder contextBuilder = new StringBuilder();

            // Extract resources from the response
            Object result = listResponse.getResult();
            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Object resourcesObj = resultMap.get("resources");

                if (resourcesObj instanceof java.util.List) {
                    java.util.List<Map<String, Object>> resources = (java.util.List<Map<String, Object>>) resourcesObj;

                    for (Map<String, Object> resource : resources) {
                        String uri = (String) resource.get("uri");
                        String name = (String) resource.get("name");

                        log.debug("Reading resource: {} ({})", name, uri);

                        // Call resources/read to get actual content
                        McpRequest readRequest = McpRequest.builder()
                                .jsonrpc("2.0")
                                .method("resources/read")
                                .params(Map.of("uri", uri))
                                .id(UUID.randomUUID().toString())
                                .build();

                        McpResponse readResponse = webClient.post()
                                .bodyValue(readRequest)
                                .retrieve()
                                .bodyToMono(McpResponse.class)
                                .block();

                        if (readResponse != null && readResponse.getError() == null) {
                            contextBuilder.append("\n=== ").append(name).append(" ===\n");
                            contextBuilder.append(readResponse.getResult().toString()).append("\n");
                            log.debug("Successfully read resource: {}", name);
                        } else {
                            log.warn("Failed to read resource {}: {}",
                                    name,
                                    readResponse != null ? readResponse.getError().getMessage() : "No response");
                        }
                    }
                }
            }

            String context = contextBuilder.toString();
            if (context.isEmpty()) {
                return "No context available from resources";
            }
            
            log.debug("Successfully fetched context from MCP server: {} characters", context.length());
            return context;

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

    /**
     * Lists available tools from the MCP server
     *
     * @return Tools list response from MCP server
     */
    public String listTools() {
        log.debug("Listing available tools from MCP server");

        try {
            McpRequest request = McpRequest.builder()
                    .jsonrpc("2.0")
                    .method("tools/list")
                    .params(Map.of())
                    .id(UUID.randomUUID().toString())
                    .build();

            McpResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(McpResponse.class)
                    .block();

            if (response != null && response.getError() == null) {
                log.debug("Successfully listed MCP tools");
                return response.getResult().toString();
            } else if (response != null && response.getError() != null) {
                log.error("MCP tools/list failed: {}", response.getError().getMessage());
                return "Error listing tools: " + response.getError().getMessage();
            }

            return "Tools list returned no result";
        } catch (Exception e) {
            log.error("Error listing MCP tools", e);
            return "Error listing MCP tools: " + e.getMessage();
        }
    }
}
