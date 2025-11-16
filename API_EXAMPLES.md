# Example API Requests

## Generate SQL Query

### Request
```bash
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Get all users who registered in the last 30 days"
  }'
```

### Expected Response
```json
{
  "sqlQuery": "SELECT * FROM users WHERE registration_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
  "context": "Database schema context from MCP server..."
}
```

## More Examples

### Example 1: Complex Join Query
```bash
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Show me all orders with customer names and total amounts from the last week"
  }'
```

### Example 2: Aggregation Query
```bash
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Count the number of orders per customer in descending order"
  }'
```

### Example 3: Update Query
```bash
curl -X POST http://localhost:8081/api/sql/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Update all inactive users to set their status to archived"
  }'
```

## Health Check
```bash
curl http://localhost:8081/api/sql/health
```

Expected: `MCP Client is running`

## Testing with httpie (alternative)
If you have httpie installed:

```bash
# Generate SQL
http POST localhost:8081/api/sql/generate prompt="Get all active users"

# Health check
http GET localhost:8081/api/sql/health
```

## Testing with Postman

1. Import the endpoint: `POST http://localhost:8081/api/sql/generate`
2. Set Content-Type header to `application/json`
3. Add request body:
   ```json
   {
     "prompt": "Your natural language query here"
   }
   ```
4. Send the request

## Notes

- Make sure the MCP server is running at `http://localhost:8080/mcp`
- Set your OpenAI API key before starting the application
- The application runs on port 8081 by default
