package com.example.mcpclient.controller;

import com.example.mcpclient.model.SqlQueryRequest;
import com.example.mcpclient.model.SqlQueryResponse;
import com.example.mcpclient.service.SqlGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
public class SqlGeneratorController {

    private final SqlGeneratorService sqlGeneratorService;

    /**
     * Generates SQL query from natural language prompt
     * 
     * @param request Request containing the user's prompt
     * @return Response with generated SQL query and context
     */
    @PostMapping("/generate")
    public ResponseEntity<SqlQueryResponse> generateSql(@RequestBody SqlQueryRequest request) {
        log.info("Received SQL generation request: {}", request.getPrompt());
        
        try {
            SqlQueryResponse response = sqlGeneratorService.generateSqlQuery(request.getPrompt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating SQL query", e);
            return ResponseEntity.internalServerError()
                    .body(SqlQueryResponse.builder()
                            .sqlQuery("Error: " + e.getMessage())
                            .context("Failed to generate SQL")
                            .build());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("MCP Client is running");
    }
}
