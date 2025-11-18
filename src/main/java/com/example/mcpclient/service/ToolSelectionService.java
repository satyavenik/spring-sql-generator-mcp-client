package com.example.mcpclient.service;

import com.example.mcpclient.client.ChatClient;
import com.example.mcpclient.client.McpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that uses LLM to decide which MCP tools to call
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolSelectionService {

    private final ChatClient chatClient;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOOL_SELECTION_PROMPT = """
            You must call one of the available functions to help with this user request.
            
            User Request: %s
            
            Choose the most appropriate function:
            - get_schema: for general SQL queries needing database structure
            - get_templates: for seeing available SQL templates 
            - get_template: for specific template by name
            
            You MUST call a function - do not provide a text response.
            """;

    // Define available MCP tools as OpenAI functions
    private static final List<Map<String, Object>> AVAILABLE_FUNCTIONS = List.of(
            Map.of(
                    "name", "get_schema",
                    "description", "Get database schema for SQL generation context",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(),
                            "required", List.of()
                    )
            ),
            Map.of(
                    "name", "get_templates",
                    "description", "Get SQL generation templates and examples",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(),
                            "required", List.of()
                    )
            ),
            Map.of(
                    "name", "get_template",
                    "description", "Get specific SQL template by name",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "name", Map.of(
                                            "type", "string",
                                            "description", "Template name to retrieve"
                                    )
                            ),
                            "required", List.of("name")
                    )
            )
    );

    /**
     * Uses LLM to decide which tool to call, then executes it
     *
     * @param userRequest The user's natural language request
     * @return Context information from the selected tool
     */
    public String selectAndCallTool(String userRequest) {
        log.info("Using LLM to select tool for request: {}", userRequest);

        try {
            // Step 1: Fetch available tools from MCP server
            List<Map<String, Object>> availableTools = fetchAvailableToolsFromMcp();
            log.debug("Fetched {} tools from MCP server", availableTools.size());

            if (availableTools.isEmpty()) {
                log.warn("No tools available from MCP server, falling back to default");
                return mcpClient.callTool("get_schema", new HashMap<>());
            }

            // Step 2: Ask LLM which tool to use with the actual available tools
            String toolSelectionPrompt = String.format(TOOL_SELECTION_PROMPT, userRequest);
            String llmResponse = chatClient.callWithFunctions(toolSelectionPrompt, availableTools);

            log.debug("LLM tool selection response: {}", llmResponse);

            // Step 3: Parse LLM response to extract tool and arguments
            JsonNode toolDecision;
            try {
                toolDecision = objectMapper.readTree(llmResponse);
            } catch (Exception parseException) {
                log.error("Failed to parse LLM response as JSON: {}", llmResponse, parseException);
                log.info("Using fallback tool selection due to JSON parse error");
                return mcpClient.callTool("get_schema", new HashMap<>());
            }

            JsonNode toolNameNode = toolDecision.get("tool");
            if (toolNameNode == null) {
                log.error("No 'tool' field found in LLM response: {}", llmResponse);
                return mcpClient.callTool("get_schema", new HashMap<>());
            }

            String toolName = toolNameNode.asText();

            Map<String, Object> arguments = new HashMap<>();
            JsonNode argumentsNode = toolDecision.get("arguments");
            if (argumentsNode != null && argumentsNode.isObject()) {
                argumentsNode.fields().forEachRemaining(entry -> {
                    arguments.put(entry.getKey(), entry.getValue().asText());
                });
            }

            log.info("LLM selected tool: {} with arguments: {}", toolName, arguments);

            // Step 4: Call the selected MCP tool
            String toolResult = mcpClient.callTool(toolName, arguments);

            log.debug("Tool execution result: {}", toolResult);
            return toolResult;

        } catch (Exception e) {
            log.error("Error in tool selection and execution", e);
            // Fallback to default schema tool
            log.info("Falling back to default get_schema tool");
            return mcpClient.callTool("get_schema", new HashMap<>());
        }
    }

    /**
     * Fetches the list of available tools from MCP server
     *
     * @return List of available tools with their schemas
     */
    private List<Map<String, Object>> fetchAvailableToolsFromMcp() {
        log.debug("Fetching available tools from MCP server");

        try {
            String toolsListResult = mcpClient.listTools();
            log.debug("Raw tools list response: {}", toolsListResult);

            // Parse the MCP response to extract tools
            JsonNode response = objectMapper.readTree(toolsListResult);
            JsonNode toolsNode = response.get("tools");

            List<Map<String, Object>> tools = new java.util.ArrayList<>();

            if (toolsNode != null && toolsNode.isArray()) {
                for (JsonNode toolNode : toolsNode) {
                    Map<String, Object> tool = new HashMap<>();
                    tool.put("name", toolNode.get("name").asText());
                    tool.put("description", toolNode.get("description").asText());

                    // Convert inputSchema to OpenAI function format
                    JsonNode inputSchema = toolNode.get("inputSchema");
                    if (inputSchema != null) {
                        tool.put("parameters", objectMapper.convertValue(inputSchema, Map.class));
                    } else {
                        tool.put("parameters", Map.of("type", "object", "properties", Map.of()));
                    }

                    tools.add(tool);
                }
            }

            log.info("Successfully parsed {} tools from MCP server", tools.size());
            return tools;

        } catch (Exception e) {
            log.error("Error fetching tools from MCP server", e);
            // Return hardcoded fallback tools
            log.info("Using fallback tool definitions");
            return AVAILABLE_FUNCTIONS;
        }
    }
}


