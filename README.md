# spring-mcp-client

A Spring Boot application that connects to an MCP (Model Context Protocol) server to fetch database schema context and uses an LLM (via ChatClient) to generate SQL queries from natural language prompts.

## Features

- **MCP Client Integration**: Connects to MCP server at `http://localhost:8080/mcp`
- **Context-Based SQL Generation**: Fetches database schema context from MCP server
- **LLM Integration**: Uses ChatClient to call OpenAI GPT-4 for SQL query generation
- **REST API**: Provides endpoints for SQL query generation

## Architecture

The application consists of:

1. **McpClient**: Communicates with MCP server to fetch context and call tools
2. **ChatClient**: Custom client wrapper for OpenAI API calls
3. **SqlGeneratorService**: Orchestrates context fetching and SQL generation
4. **SqlGeneratorController**: REST API endpoints

## Configuration

Configure the application in `src/main/resources/application.properties`:

```properties
# MCP Server Configuration
mcp.server.url=http://localhost:8080/mcp

# OpenAI Configuration
openai.api-key=${OPENAI_API_KEY:your-api-key-here}
openai.model=gpt-4

# Server Configuration
server.port=8081
```

Set your OpenAI API key as an environment variable:
```bash
export OPENAI_API_KEY=your-actual-api-key
```

## Building and Running

### Build
```bash
./gradlew clean build
```

### Run
```bash
./gradlew bootRun
```

Or run the JAR:
```bash
java -jar build/libs/spring-mcp-client-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Generate SQL Query
```bash
POST http://localhost:8081/api/sql/generate
Content-Type: application/json

{
  "prompt": "Get all users who registered in the last 30 days"
}
```

Response:
```json
{
  "sqlQuery": "SELECT * FROM users WHERE registration_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
  "context": "Database schema context from MCP server"
}
```

### Health Check
```bash
GET http://localhost:8081/api/sql/health
```

## How It Works

1. User sends a natural language prompt via the REST API
2. The application calls the MCP server to fetch relevant database schema context
3. The context and prompt are combined into a structured prompt
4. ChatClient sends the prompt to OpenAI GPT-4
5. The LLM generates a SQL query based on the context
6. The application returns the generated SQL query and context

## Requirements

- Java 17 or higher
- Gradle 8.5 or higher
- OpenAI API key
- MCP server running at http://localhost:8080/mcp

## Project Structure

```
src/main/java/com/example/mcpclient/
├── McpClientApplication.java       # Main Spring Boot application
├── client/
│   ├── McpClient.java              # MCP server client
│   └── ChatClient.java             # OpenAI API client
├── controller/
│   └── SqlGeneratorController.java # REST API endpoints
├── model/
│   ├── McpRequest.java             # MCP request model
│   ├── McpResponse.java            # MCP response model
│   ├── SqlQueryRequest.java        # API request model
│   └── SqlQueryResponse.java       # API response model
└── service/
    └── SqlGeneratorService.java    # SQL generation orchestration
```

## Testing

Run tests:
```bash
./gradlew test
```

## License

MIT

