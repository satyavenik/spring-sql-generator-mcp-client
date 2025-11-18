package com.example.mcpclient.service;

import com.example.mcpclient.client.ChatClient;
import com.example.mcpclient.client.McpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolSelectionServiceErrorHandlingTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private McpClient mcpClient;

    @InjectMocks
    private ToolSelectionService toolSelectionService;

    @Test
    void testSelectAndCallTool_HandlesJsonParseError() {
        // Given - LLM returns text instead of JSON (the error case we're fixing)
        String userRequest = "Generate SQL for recently registered users";
        String toolsListResponse = """
                {
                    "tools": [
                        {
                            "name": "get_schema",
                            "description": "Get database schema for SQL generation context",
                            "inputSchema": {
                                "type": "object",
                                "properties": {},
                                "required": []
                            }
                        }
                    ]
                }
                """;
        String llmTextResponse = "The most appropriate MCP tool to call for this user request would be `functions.get_template`.";
        String fallbackContext = "Database schema: users table with id, name, email, created_at columns";

        when(mcpClient.listTools()).thenReturn(toolsListResponse);
        when(chatClient.callWithFunctions(anyString(), anyList())).thenReturn(llmTextResponse);
        when(mcpClient.callTool(eq("get_schema"), any())).thenReturn(fallbackContext);

        // When
        String result = toolSelectionService.selectAndCallTool(userRequest);

        // Then
        assertEquals(fallbackContext, result);
        verify(mcpClient).listTools();
        verify(chatClient).callWithFunctions(contains(userRequest), anyList());
        verify(mcpClient).callTool("get_schema", new HashMap<>());
    }

    @Test
    void testSelectAndCallTool_HandlesEmptyToolField() {
        // Given - LLM returns JSON but without "tool" field
        String userRequest = "Generate SQL query";
        String toolsListResponse = """
                {
                    "tools": [
                        {
                            "name": "get_schema",
                            "description": "Get database schema",
                            "inputSchema": {"type": "object", "properties": {}}
                        }
                    ]
                }
                """;
        String llmResponseMissingTool = """
                {
                    "function": "get_schema",
                    "arguments": {}
                }
                """;
        String fallbackContext = "Default schema context";

        when(mcpClient.listTools()).thenReturn(toolsListResponse);
        when(chatClient.callWithFunctions(anyString(), anyList())).thenReturn(llmResponseMissingTool);
        when(mcpClient.callTool(eq("get_schema"), any())).thenReturn(fallbackContext);

        // When
        String result = toolSelectionService.selectAndCallTool(userRequest);

        // Then
        assertEquals(fallbackContext, result);
        verify(mcpClient).callTool("get_schema", new HashMap<>());
    }

    @Test
    void testSelectAndCallTool_HandlesExceptionInToolExecution() {
        // Given
        String userRequest = "Show me data";
        String toolsListResponse = """
                {
                    "tools": [
                        {
                            "name": "get_schema",
                            "description": "Get database schema",
                            "inputSchema": {"type": "object", "properties": {}}
                        }
                    ]
                }
                """;
        String llmResponse = """
                {
                    "tool": "get_schema",
                    "arguments": {}
                }
                """;

        when(mcpClient.listTools()).thenReturn(toolsListResponse);
        when(chatClient.callWithFunctions(anyString(), anyList())).thenReturn(llmResponse);
        when(mcpClient.callTool(eq("get_schema"), any()))
                .thenThrow(new RuntimeException("MCP server connection failed"))
                .thenReturn("Fallback schema data");

        // When
        String result = toolSelectionService.selectAndCallTool(userRequest);

        // Then
        assertEquals("Fallback schema data", result);
        verify(mcpClient, times(2)).callTool("get_schema", new HashMap<>());
    }
}
