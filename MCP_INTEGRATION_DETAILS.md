# MCP Integration Details

This document provides detailed information about how the Spring Boot MCP Client integrates with MCP servers.

## Table of Contents

- [MCP Protocol Overview](#mcp-protocol-overview)
- [Communication Flow](#communication-flow)
- [MCP Client Implementation](#mcp-client-implementation)
- [JSON-RPC 2.0 Specification](#json-rpc-20-specification)
- [Available MCP Methods](#available-mcp-methods)
- [Example Scenarios](#example-scenarios)
- [Error Handling](#error-handling)

## MCP Protocol Overview

The Model Context Protocol (MCP) is a JSON-RPC 2.0 based protocol that enables standardized communication between clients and servers for context retrieval and tool execution.

### Key Features

- **JSON-RPC 2.0**: Standard protocol for remote procedure calls
- **Resources**: Read-only data sources (database schemas, documentation, etc.)
- **Tools**: Executable functions on the server
- **HTTP Transport**: Communication over HTTP/HTTPS

## Communication Flow

### 1. Application Startup

```
Spring Boot Application
    ↓
WebClient Initialization
    ↓
MCP Server URL Configuration (http://localhost:8080/mcp)
```

### 2. SQL Generation Request Flow

```
1. User Request → Controller
   POST /api/sql/generate
   { "prompt": "Get all users..." }

2. Controller → Service
   SqlGeneratorService.generateSqlQuery(prompt)

3. Service → MCP Client
   McpClient.fetchContext(prompt)

4. MCP Client → MCP Server (Step 1)
   JSON-RPC: resources/list
   Purpose: Get available database schema resources

5. MCP Server → MCP Client
   Returns: List of resources with URIs

6. MCP Client → MCP Server (Step 2, per resource)
   JSON-RPC: resources/read
   Purpose: Read actual schema content

7. MCP Server → MCP Client
   Returns: Schema content for each resource

8. Service → Chat Client
   Combines: prompt + context → LLM

9. Chat Client → OpenAI API
   Sends: Full prompt with schema context

10. OpenAI API → Chat Client
    Returns: Generated SQL query

11. Service → Controller
    Returns: SqlQueryResponse

12. Controller → User
    HTTP 200 OK with SQL and context
```

## MCP Client Implementation

### Key Components

#### 1. WebClient Configuration

```java
private final WebClient webClient;

public McpClient(@Value("${mcp.server.url}") String mcpServerUrl) {
    this.webClient = WebClient.builder()
            .baseUrl(mcpServerUrl)
            .build();
}
```

**Configuration:**
- Base URL: Configured via `mcp.server.url` property
- Default: `http://localhost:8080/mcp`
- Transport: HTTP POST requests
- Content-Type: `application/json`

#### 2. Context Fetching Logic

```java
public String fetchContext(String prompt) {
    // Step 1: List all resources
    McpRequest listRequest = McpRequest.builder()
            .jsonrpc("2.0")
            .method("resources/list")
            .params(Map.of())
            .id(UUID.randomUUID().toString())
            .build();

    // Step 2: Read each resource
    // ... (iterates through resources)
    
    // Step 3: Combine all context
    return contextBuilder.toString();
}
```

## JSON-RPC 2.0 Specification

### Request Format

```json
{
  "jsonrpc": "2.0",
  "method": "method_name",
  "params": {
    "param1": "value1",
    "param2": "value2"
  },
  "id": "unique-request-id"
}
```

**Fields:**
- `jsonrpc`: Always "2.0" (protocol version)
- `method`: Name of the method to call
- `params`: Object or array of parameters
- `id`: Unique identifier for request/response matching

### Success Response Format

```json
{
  "jsonrpc": "2.0",
  "result": {
    "data": "response data"
  },
  "id": "unique-request-id"
}
```

**Fields:**
- `jsonrpc`: Always "2.0"
- `result`: The result of the method call
- `id`: Matches the request ID

### Error Response Format

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": {
      "additional": "information"
    }
  },
  "id": "unique-request-id"
}
```

**Fields:**
- `jsonrpc`: Always "2.0"
- `error`: Error object with code, message, and optional data
- `id`: Matches the request ID (or null if ID couldn't be determined)

### Standard Error Codes

| Code | Message | Meaning |
|------|---------|---------|
| -32700 | Parse error | Invalid JSON was received |
| -32600 | Invalid Request | JSON-RPC request is not valid |
| -32601 | Method not found | Method does not exist |
| -32602 | Invalid params | Invalid method parameters |
| -32603 | Internal error | Internal JSON-RPC error |
| -32000 to -32099 | Server error | Implementation-defined server errors |

## Available MCP Methods

### 1. resources/list

**Purpose:** List all available resources on the MCP server

**Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/list",
  "params": {},
  "id": "req-001"
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "result": {
    "resources": [
      {
        "uri": "schema://users",
        "name": "Users Table Schema",
        "description": "Schema definition for users table",
        "mimeType": "text/plain"
      },
      {
        "uri": "schema://orders",
        "name": "Orders Table Schema",
        "description": "Schema definition for orders table",
        "mimeType": "text/plain"
      }
    ]
  }
}
```

**Fields Explained:**
- `uri`: Unique identifier for the resource
- `name`: Human-readable name
- `description`: Description of the resource
- `mimeType`: Content type (e.g., text/plain, application/json)

### 2. resources/read

**Purpose:** Read the content of a specific resource

**Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "schema://users"
  },
  "id": "req-002"
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "req-002",
  "result": {
    "contents": [
      {
        "uri": "schema://users",
        "mimeType": "text/plain",
        "text": "Table: users\nColumns:\n- id (INTEGER, PRIMARY KEY)\n- username (VARCHAR(255), NOT NULL)\n- email (VARCHAR(255), UNIQUE)\n- registration_date (TIMESTAMP)\n- is_active (BOOLEAN)"
      }
    ]
  }
}
```

**Fields Explained:**
- `contents`: Array of content objects
- `uri`: Resource URI
- `mimeType`: Content type
- `text`: Actual content (for text-based resources)

### 3. tools/call (Optional)

**Purpose:** Execute a tool on the MCP server

**Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "query_analyzer",
    "arguments": {
      "query": "SELECT * FROM users"
    }
  },
  "id": "req-003"
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "req-003",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Query analysis: Simple SELECT statement on users table"
      }
    ]
  }
}
```

## Example Scenarios

### Scenario 1: Successful Context Fetch

**Timeline:**

```
T+0ms: Client sends resources/list request
T+50ms: Server responds with 3 resources
T+51ms: Client sends resources/read for resource 1
T+70ms: Server responds with schema content
T+71ms: Client sends resources/read for resource 2
T+90ms: Server responds with schema content
T+91ms: Client sends resources/read for resource 3
T+110ms: Server responds with schema content
T+111ms: Client combines all contexts
T+112ms: Returns aggregated context to service
```

**Request 1:**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/list",
  "params": {},
  "id": "a1b2c3d4"
}
```

**Response 1:**
```json
{
  "jsonrpc": "2.0",
  "id": "a1b2c3d4",
  "result": {
    "resources": [
      {
        "uri": "schema://users",
        "name": "Users Table"
      },
      {
        "uri": "schema://orders",
        "name": "Orders Table"
      },
      {
        "uri": "schema://products",
        "name": "Products Table"
      }
    ]
  }
}
```

**Request 2 (for each resource):**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "schema://users"
  },
  "id": "e5f6g7h8"
}
```

**Response 2:**
```json
{
  "jsonrpc": "2.0",
  "id": "e5f6g7h8",
  "result": {
    "contents": [
      {
        "uri": "schema://users",
        "mimeType": "text/plain",
        "text": "CREATE TABLE users (\n  id INT PRIMARY KEY,\n  username VARCHAR(255),\n  email VARCHAR(255)\n)"
      }
    ]
  }
}
```

**Final Context:**
```
=== Users Table ===
CREATE TABLE users (
  id INT PRIMARY KEY,
  username VARCHAR(255),
  email VARCHAR(255)
)

=== Orders Table ===
CREATE TABLE orders (
  order_id INT PRIMARY KEY,
  user_id INT,
  total DECIMAL(10,2)
)

=== Products Table ===
CREATE TABLE products (
  product_id INT PRIMARY KEY,
  name VARCHAR(255),
  price DECIMAL(10,2)
)
```

### Scenario 2: Error Handling

**Case: Method Not Found**

**Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "invalid/method",
  "params": {},
  "id": "xyz123"
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "xyz123",
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": {
      "availableMethods": ["resources/list", "resources/read", "tools/call"]
    }
  }
}
```

**Client Handling:**
```java
if (response.getError() != null) {
    log.error("MCP server error: {}", response.getError().getMessage());
    return "Error: " + response.getError().getMessage();
}
```

### Scenario 3: Resource Not Found

**Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "schema://nonexistent"
  },
  "id": "abc789"
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "abc789",
  "error": {
    "code": -32000,
    "message": "Resource not found",
    "data": {
      "uri": "schema://nonexistent"
    }
  }
}
```

## Error Handling

### Client-Side Error Handling

#### 1. Connection Errors

```java
try {
    McpResponse response = webClient.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(McpResponse.class)
            .block();
} catch (WebClientException e) {
    log.error("Failed to connect to MCP server", e);
    return "Error: Cannot connect to MCP server";
}
```

**Common Causes:**
- MCP server is not running
- Wrong URL/port configuration
- Network connectivity issues
- Firewall blocking the connection

#### 2. MCP Protocol Errors

```java
if (response != null && response.getError() != null) {
    String errorMessage = response.getError().getMessage();
    int errorCode = response.getError().getCode();
    log.error("MCP error {}: {}", errorCode, errorMessage);
    
    // Handle specific error codes
    switch (errorCode) {
        case -32601:
            return "Error: Requested method not supported by MCP server";
        case -32602:
            return "Error: Invalid parameters provided";
        default:
            return "Error: " + errorMessage;
    }
}
```

#### 3. Empty Response Handling

```java
if (response == null) {
    log.error("Received null response from MCP server");
    return "Error: No response from MCP server";
}

if (response.getResult() == null) {
    log.warn("MCP server returned no result");
    return "No context available";
}
```

### Best Practices

1. **Timeouts**: Configure appropriate timeouts for WebClient
2. **Retries**: Implement retry logic for transient failures
3. **Logging**: Log all MCP interactions for debugging
4. **Validation**: Validate response structure before processing
5. **Fallback**: Provide default context if MCP server is unavailable

### Configuration for Resilience

```properties
# application.properties
mcp.server.url=http://localhost:8080/mcp
mcp.timeout.connect=5000
mcp.timeout.read=10000
mcp.retry.max-attempts=3
mcp.retry.backoff=1000
```

## Testing MCP Integration

### Manual Testing with cURL

#### Test resources/list

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "resources/list",
    "params": {},
    "id": "test-001"
  }'
```

#### Test resources/read

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "resources/read",
    "params": {
      "uri": "schema://users"
    },
    "id": "test-002"
  }'
```

### Testing with Postman

1. Create a new POST request to `http://localhost:8080/mcp`
2. Set Content-Type header to `application/json`
3. Add the JSON-RPC request body
4. Send and verify the response structure

### Integration Testing

Create automated tests to verify MCP integration:

```java
@Test
public void testMcpContextFetch() {
    String context = mcpClient.fetchContext("test prompt");
    assertNotNull(context);
    assertTrue(context.contains("Table:"));
}

@Test
public void testMcpErrorHandling() {
    // Simulate MCP server unavailable
    // Verify graceful error handling
}
```

## Monitoring and Debugging

### Enable Debug Logging

```properties
logging.level.com.example.mcpclient.client.McpClient=DEBUG
logging.level.org.springframework.web.reactive.function.client=TRACE
```

### Log Output Examples

**Successful Request:**
```
DEBUG McpClient - Fetching context from MCP server for prompt: Get all users
DEBUG McpClient - Resources list response: {resources=[...]}
DEBUG McpClient - Reading resource: Users Table Schema (schema://users)
DEBUG McpClient - Successfully read resource: Users Table Schema
DEBUG McpClient - Successfully fetched context: 1234 characters
```

**Error Case:**
```
ERROR McpClient - MCP server returned error on resources/list: Method not found
ERROR McpClient - Error calling MCP server: Connection refused
```

---

**For more information:**
- JSON-RPC 2.0 Specification: https://www.jsonrpc.org/specification
- Model Context Protocol: [MCP Documentation]
- Spring WebFlux: https://docs.spring.io/spring-framework/reference/web/webflux.html

