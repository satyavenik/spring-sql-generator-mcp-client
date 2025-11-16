# Spring Boot MCP Client for SQL Generation

A Spring Boot application that acts as a client to interact with Model Context Protocol (MCP) servers to generate SQL queries from natural language prompts using Large Language Models (LLMs).

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [How It Works](#how-it-works)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Request & Response Examples](#request--response-examples)
- [Postman Collection](#postman-collection)
- [MCP Server Communication](#mcp-server-communication)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

This application provides a REST API that:
1. Accepts natural language prompts from users
2. Fetches database schema context from an MCP server
3. Uses an LLM (GPT-4) to generate SQL queries based on the prompt and context
4. Returns the generated SQL query with the relevant context

## 🏗️ Architecture

```
User/Client
    ↓
Spring Boot MCP Client (This Application - Port 8081)
    ↓
    ├─→ MCP Server (Port 8080) - Provides database schema context
    │
    └─→ OpenAI GPT-4 - Generates SQL from prompt + context
```

### Components

- **SqlGeneratorController**: REST endpoint handling HTTP requests
- **SqlGeneratorService**: Business logic orchestrating the SQL generation
- **McpClient**: Client for communicating with MCP server (JSON-RPC 2.0)
- **ChatClient**: Client for calling OpenAI's LLM API
- **Models**: Request/Response DTOs with Swagger documentation

## 🔄 How It Works

### Step-by-Step Process

1. **User sends a prompt** via REST API:
   ```
   POST /api/sql/generate
   Body: { "prompt": "Get all users who registered in the last 30 days" }
   ```

2. **Service layer receives the request** and calls `SqlGeneratorService.generateSqlQuery()`

3. **MCP Client fetches context** from the MCP server:
   - Sends `resources/list` request to MCP server
   - Receives list of available database schema resources
   - For each resource, sends `resources/read` request
   - Collects all schema information as context

4. **Chat Client calls LLM**:
   - Combines user prompt with fetched context
   - Sends to OpenAI GPT-4 with specific instructions
   - Receives generated SQL query

5. **Response returned** to user with:
   - Generated SQL query
   - Database schema context used

## ✅ Prerequisites

- Java 17 or higher
- Maven 3.6+ or Gradle 7.0+
- MCP Server running on port 8080 (or configured port)
- OpenAI API Key (for GPT-4 access)

## 📦 Installation

### Clone the Repository

```bash
git clone <repository-url>
cd spring-sql-generator-mcp-client
```

### Build the Project

#### Using Maven:
```bash
mvn clean install
```

#### Using Gradle:
```bash
./gradlew build
```

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Application Name
spring.application.name=mcp-client

# MCP Server Configuration
mcp.server.url=http://localhost:8080/mcp

# OpenAI Configuration
openai.api-key=${OPENAI_API_KEY:your-api-key-here}
openai.model=gpt-4

# Server Port
server.port=8081

# Logging
logging.level.com.example.mcpclient=DEBUG

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
```

### Environment Variables

Set your OpenAI API key:

**Windows (CMD):**
```cmd
set OPENAI_API_KEY=sk-your-actual-api-key
```

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY="sk-your-actual-api-key"
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY=sk-your-actual-api-key
```

## 🚀 Running the Application

### Using Maven:
```bash
mvn spring-boot:run
```

### Using Gradle:
```bash
./gradlew bootRun
```

### Using JAR:
```bash
java -jar target/spring-sql-generator-mcp-client-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8081`

## 📚 API Documentation

### Swagger UI

Access the interactive API documentation:
```
http://localhost:8081/swagger-ui.html
```

### API Endpoints

#### 1. Generate SQL Query

**Endpoint:** `POST /api/sql/generate`

**Description:** Generates SQL query from natural language prompt

**Request Body:**
```json
{
  "prompt": "string"
}
```

**Response:**
```json
{
  "sqlQuery": "string",
  "context": "string"
}
```

#### 2. Health Check

**Endpoint:** `GET /api/sql/health`

**Description:** Check if the service is running

**Response:** `200 OK` with text "MCP Client is running"

## 📝 Request & Response Examples

### Example 1: Simple User Query

**Request:**
```http
POST http://localhost:8081/api/sql/generate
Content-Type: application/json

{
  "prompt": "Get all users who registered in the last 30 days"
}
```

**Response:**
```json
{
  "sqlQuery": "SELECT * FROM users WHERE registration_date >= DATE_SUB(NOW(), INTERVAL 30 DAY);",
  "context": "\n=== Database Schema - Users Table ===\nTable: users\nColumns:\n- id (INTEGER, PRIMARY KEY)\n- username (VARCHAR(255), NOT NULL)\n- email (VARCHAR(255), UNIQUE, NOT NULL)\n- registration_date (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)\n- is_active (BOOLEAN, DEFAULT TRUE)\n"
}
```

### Example 2: Join Query

**Request:**
```http
POST http://localhost:8081/api/sql/generate
Content-Type: application/json

{
  "prompt": "Show all orders with customer names and total amounts for orders over $1000"
}
```

**Response:**
```json
{
  "sqlQuery": "SELECT c.name, o.order_id, o.total_amount FROM customers c INNER JOIN orders o ON c.customer_id = o.customer_id WHERE o.total_amount > 1000;",
  "context": "\n=== Database Schema - Customers Table ===\nTable: customers\nColumns:\n- customer_id (INTEGER, PRIMARY KEY)\n- name (VARCHAR(255), NOT NULL)\n- email (VARCHAR(255))\n\n=== Database Schema - Orders Table ===\nTable: orders\nColumns:\n- order_id (INTEGER, PRIMARY KEY)\n- customer_id (INTEGER, FOREIGN KEY REFERENCES customers)\n- total_amount (DECIMAL(10,2))\n- order_date (DATE)\n"
}
```

### Example 3: Aggregate Query

**Request:**
```http
POST http://localhost:8081/api/sql/generate
Content-Type: application/json

{
  "prompt": "Count how many products are in each category"
}
```

**Response:**
```json
{
  "sqlQuery": "SELECT category, COUNT(*) as product_count FROM products GROUP BY category;",
  "context": "\n=== Database Schema - Products Table ===\nTable: products\nColumns:\n- product_id (INTEGER, PRIMARY KEY)\n- name (VARCHAR(255), NOT NULL)\n- category (VARCHAR(100))\n- price (DECIMAL(10,2))\n- stock_quantity (INTEGER)\n"
}
```

## 🔧 MCP Server Communication

### MCP Protocol Details

The application communicates with the MCP server using JSON-RPC 2.0 protocol over HTTP.

#### 1. Listing Available Resources

**MCP Request to Server:**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/list",
  "params": {},
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**MCP Response from Server:**
```json
{
  "jsonrpc": "2.0",
  "id": "550e8400-e29b-41d4-a716-446655440000",
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

#### 2. Reading Resource Content

**MCP Request to Server:**
```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "schema://users"
  },
  "id": "550e8400-e29b-41d4-a716-446655440001"
}
```

**MCP Response from Server:**
```json
{
  "jsonrpc": "2.0",
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "result": {
    "contents": [
      {
        "uri": "schema://users",
        "mimeType": "text/plain",
        "text": "Table: users\nColumns:\n- id (INTEGER, PRIMARY KEY)\n- username (VARCHAR(255), NOT NULL)\n- email (VARCHAR(255), UNIQUE)\n- registration_date (TIMESTAMP)"
      }
    ]
  }
}
```

#### 3. Error Response Example

**MCP Error Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": {
      "method": "invalid/method"
    }
  }
}
```

## 📮 Postman Collection

### Import This Collection

Create a new Postman collection and add these requests:

#### 1. Generate SQL - Simple Query

```json
{
  "name": "Generate SQL - Simple User Query",
  "request": {
    "method": "POST",
    "header": [
      {
        "key": "Content-Type",
        "value": "application/json"
      }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n  \"prompt\": \"Get all users who registered in the last 30 days\"\n}"
    },
    "url": {
      "raw": "http://localhost:8081/api/sql/generate",
      "protocol": "http",
      "host": ["localhost"],
      "port": "8081",
      "path": ["api", "sql", "generate"]
    }
  },
  "response": [
    {
      "name": "Success Response",
      "status": "OK",
      "code": 200,
      "body": "{\n  \"sqlQuery\": \"SELECT * FROM users WHERE registration_date >= DATE_SUB(NOW(), INTERVAL 30 DAY);\",\n  \"context\": \"\\n=== Database Schema - Users Table ===\\nTable: users\\nColumns:\\n- id (INTEGER, PRIMARY KEY)\\n- username (VARCHAR(255), NOT NULL)\\n- email (VARCHAR(255), UNIQUE, NOT NULL)\\n- registration_date (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)\\n- is_active (BOOLEAN, DEFAULT TRUE)\\n\"\n}"
    }
  ]
}
```

#### 2. Generate SQL - Join Query

```json
{
  "name": "Generate SQL - Orders with Customer Names",
  "request": {
    "method": "POST",
    "header": [
      {
        "key": "Content-Type",
        "value": "application/json"
      }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n  \"prompt\": \"Show all orders with customer names and total amounts for orders over $1000\"\n}"
    },
    "url": {
      "raw": "http://localhost:8081/api/sql/generate",
      "protocol": "http",
      "host": ["localhost"],
      "port": "8081",
      "path": ["api", "sql", "generate"]
    }
  },
  "response": [
    {
      "name": "Success Response",
      "status": "OK",
      "code": 200,
      "body": "{\n  \"sqlQuery\": \"SELECT c.name, o.order_id, o.total_amount FROM customers c INNER JOIN orders o ON c.customer_id = o.customer_id WHERE o.total_amount > 1000;\",\n  \"context\": \"\\n=== Database Schema - Customers Table ===\\nTable: customers\\nColumns:\\n- customer_id (INTEGER, PRIMARY KEY)\\n- name (VARCHAR(255), NOT NULL)\\n- email (VARCHAR(255))\\n\\n=== Database Schema - Orders Table ===\\nTable: orders\\nColumns:\\n- order_id (INTEGER, PRIMARY KEY)\\n- customer_id (INTEGER, FOREIGN KEY REFERENCES customers)\\n- total_amount (DECIMAL(10,2))\\n- order_date (DATE)\\n\"\n}"
    }
  ]
}
```

#### 3. Generate SQL - Aggregate Query

```json
{
  "name": "Generate SQL - Product Count by Category",
  "request": {
    "method": "POST",
    "header": [
      {
        "key": "Content-Type",
        "value": "application/json"
      }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n  \"prompt\": \"Count how many products are in each category\"\n}"
    },
    "url": {
      "raw": "http://localhost:8081/api/sql/generate",
      "protocol": "http",
      "host": ["localhost"],
      "port": "8081",
      "path": ["api", "sql", "generate"]
    }
  },
  "response": [
    {
      "name": "Success Response",
      "status": "OK",
      "code": 200,
      "body": "{\n  \"sqlQuery\": \"SELECT category, COUNT(*) as product_count FROM products GROUP BY category;\",\n  \"context\": \"\\n=== Database Schema - Products Table ===\\nTable: products\\nColumns:\\n- product_id (INTEGER, PRIMARY KEY)\\n- name (VARCHAR(255), NOT NULL)\\n- category (VARCHAR(100))\\n- price (DECIMAL(10,2))\\n- stock_quantity (INTEGER)\\n\"\n}"
    }
  ]
}
```

#### 4. Generate SQL - Complex Filter

```json
{
  "name": "Generate SQL - Active Users with Recent Orders",
  "request": {
    "method": "POST",
    "header": [
      {
        "key": "Content-Type",
        "value": "application/json"
      }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n  \"prompt\": \"Find all active users who have placed at least 3 orders in the last 6 months\"\n}"
    },
    "url": {
      "raw": "http://localhost:8081/api/sql/generate",
      "protocol": "http",
      "host": ["localhost"],
      "port": "8081",
      "path": ["api", "sql", "generate"]
    }
  },
  "response": [
    {
      "name": "Success Response",
      "status": "OK",
      "code": 200,
      "body": "{\n  \"sqlQuery\": \"SELECT u.user_id, u.username, u.email, COUNT(o.order_id) as order_count FROM users u INNER JOIN orders o ON u.user_id = o.user_id WHERE u.is_active = TRUE AND o.order_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH) GROUP BY u.user_id, u.username, u.email HAVING COUNT(o.order_id) >= 3;\",\n  \"context\": \"\\n=== Database Schema ===\\nMultiple tables with relationships...\\n\"\n}"
    }
  ]
}
```

#### 5. Health Check

```json
{
  "name": "Health Check",
  "request": {
    "method": "GET",
    "header": [],
    "url": {
      "raw": "http://localhost:8081/api/sql/health",
      "protocol": "http",
      "host": ["localhost"],
      "port": "8081",
      "path": ["api", "sql", "health"]
    }
  },
  "response": [
    {
      "name": "Success Response",
      "status": "OK",
      "code": 200,
      "body": "MCP Client is running"
    }
  ]
}
```

### cURL Examples

```bash
# Example 1: Simple Query
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Get all users who registered in the last 30 days"}'

# Example 2: Join Query
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Show all orders with customer names and total amounts for orders over $1000"}'

# Example 3: Aggregate Query
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Count how many products are in each category"}'

# Example 4: Health Check
curl http://localhost:8081/api/sql/health
```

## 🐛 Troubleshooting

### Common Issues

#### 1. MCP Server Connection Failed

**Error:** `Error fetching context from MCP server: Connection refused`

**Solution:**
- Ensure MCP server is running on the configured port (default: 8080)
- Check `mcp.server.url` in `application.properties`
- Verify network connectivity

#### 2. OpenAI API Key Issues

**Error:** `401 Unauthorized` from OpenAI

**Solution:**
- Verify your `OPENAI_API_KEY` environment variable is set correctly
- Check that your API key is valid and has sufficient credits
- Ensure the key starts with `sk-`

#### 3. Empty Context Response

**Error:** `No context available from resources`

**Solution:**
- Verify MCP server is properly configured with database schema resources
- Check MCP server logs for errors
- Test MCP server directly with JSON-RPC requests

#### 4. Port Already in Use

**Error:** `Port 8081 is already in use`

**Solution:**
- Change the port in `application.properties`: `server.port=8082`
- Or stop the process using port 8081

### Debug Mode

Enable detailed logging:

```properties
logging.level.com.example.mcpclient=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG
```

## 📄 License

[Add your license information here]

## 🤝 Contributing

[Add contribution guidelines here]

## 📧 Contact

[Add contact information here]

---

**Built with:** Spring Boot 3.2.1, Java 17, WebFlux, OpenAI GPT-4, MCP Protocol

