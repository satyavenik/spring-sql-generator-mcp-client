package com.example.mcpclient.service;

import com.example.mcpclient.client.ChatClient;
import com.example.mcpclient.client.McpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolSelectionServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private McpClient mcpClient;

    @InjectMocks
    private ToolSelectionService toolSelectionService;

    @Test
    void testSelectAndCallTool_SchemaSelection() {
        // Given
        String userRequest = "Show me all customers from the database";
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
        String llmResponse = """
                {
                    "tool": "get_schema",
                    "arguments": {}
                }
                """;
        String expectedContext = "Database schema: customers table with id, name, email columns";

        when(mcpClient.listTools()).thenReturn(toolsListResponse);
        when(chatClient.callWithFunctions(anyString(), anyList())).thenReturn(llmResponse);
        when(mcpClient.callTool(eq("get_schema"), any())).thenReturn(expectedContext);

        // When
        String result = toolSelectionService.selectAndCallTool(userRequest);

        // Then
        assertEquals(expectedContext, result);
        verify(mcpClient).listTools();
        verify(chatClient).callWithFunctions(contains(userRequest), anyList());
        verify(mcpClient).callTool("get_schema", new HashMap<>());
    }

    @Test
    void testSelectAndCallTool_TemplateSelection() {
        // Given
        String userRequest = "Get me the customer report template";
        String toolsListResponse = """
                {
                    "tools": [
                        {
                            "name": "get_template",
                            "description": "Get specific SQL template by name",
                            "inputSchema": {
                                "type": "object",
                                "properties": {
                                    "name": {
                                        "type": "string",
                                        "description": "Template name"
                                    }
                                },
                                "required": ["name"]
                            }
                        }
                    ]
                }
                """;
        String llmResponse = """
                {
                    "tool": "get_template",
                    "arguments": {
                        "name": "customer_report"
                    }
                }
                """;
        String expectedContext = "Template: SELECT * FROM customers WHERE active = 1";
        Map<String, Object> expectedArgs = Map.of("name", "customer_report");

        when(mcpClient.listTools()).thenReturn(toolsListResponse);
        when(chatClient.callWithFunctions(anyString(), anyList())).thenReturn(llmResponse);
        when(mcpClient.callTool(eq("get_template"), eq(expectedArgs))).thenReturn(expectedContext);

        // When
        String result = toolSelectionService.selectAndCallTool(userRequest);

        // Then
        assertEquals(expectedContext, result);
        verify(mcpClient).listTools();
        verify(chatClient).callWithFunctions(contains(userRequest), anyList());
        verify(mcpClient).callTool("get_template", expectedArgs);
    }

    @Test
    void testSelectAndCallTool_FallbackOnError() {
        // Given
        String userRequest = "Show me data";
        String fallbackContext = "Default schema context";

        when(mcpClient.listTools()).thenThrow(new RuntimeException("MCP server error"));
        when(mcpClient.callTool(eq("get_schema"), any())).thenReturn(fallbackContext);

        // When
        String result = toolSelectionService.selectAndCallTool(userRequest);

        // Then
        assertEquals(fallbackContext, result);
        verify(mcpClient).listTools();
        verify(mcpClient).callTool("get_schema", new HashMap<>());
    }
}
