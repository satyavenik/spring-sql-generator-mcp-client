# Updated LLM-Based Tool Selection Flow

## Pattern Implementation

Your repo now follows the exact pattern you described, with an improvement:

```
User → "Check my balance"
↓ 
1. Client calls MCP server → tools/list (to get available tools)
↓ 
2. MCP server returns → available tools list with schemas
↓ 
3. Client sends prompt + available tools to LLM
↓ 
4. LLM decides → use tool: getBalance(accountId=9876)
↓ 
5. Client calls MCP server → tools/call {name: "getBalance", arguments: {...}}
↓ 
6. MCP server returns → "$5,200.00"
↓ 
7. Client formats response (via LLM if needed)
↓ 
8. User sees → "Your balance is $5,200.00"
```

## Code Flow

### 1. ToolSelectionService.selectAndCallTool()
- **Step 1**: Calls `mcpClient.listTools()` to get actual available tools from MCP server
- **Step 2**: Calls `chatClient.callWithFunctions(prompt, availableTools)` to let LLM choose
- **Step 3**: Parses LLM response to extract tool name and arguments  
- **Step 4**: Calls `mcpClient.callTool(toolName, arguments)` to execute selected tool

### 2. McpClient Methods
- `listTools()`: Calls MCP server with `tools/list` method
- `callTool(name, args)`: Calls MCP server with `tools/call` method

### 3. ChatClient Methods  
- `callWithFunctions()`: Uses OpenAI function calling to let LLM pick tools
- `call()`: Regular LLM call for final SQL generation

## Key Improvements

1. **Dynamic Tool Discovery**: Instead of hardcoded tools, fetches actual available tools from MCP server
2. **LLM Tool Selection**: Uses OpenAI function calling for better tool selection
3. **Fallback Handling**: Falls back to default schema tool if MCP server is unavailable
4. **Proper Error Handling**: Comprehensive error handling at each step

## Example Request Flow

```java
// User request: "Generate SQL to find all active customers"

// 1. ToolSelectionService fetches available tools
mcpClient.listTools() → {
  "tools": [
    {"name": "get_schema", "description": "Get database schema"},
    {"name": "get_templates", "description": "Get SQL templates"},
    {"name": "get_template", "description": "Get specific template"}
  ]
}

// 2. LLM selects appropriate tool
chatClient.callWithFunctions(prompt, tools) → {
  "tool": "get_schema",
  "arguments": {}
}

// 3. Execute selected tool
mcpClient.callTool("get_schema", {}) → "Database schema: customers table..."

// 4. Generate final SQL using context
chatClient.call(prompt + schema) → "SELECT * FROM customers WHERE status = 'active'"
```

This implementation is now **truly LLM-driven tool selection** as you requested!
