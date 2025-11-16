package com.example.mcpclient.controller;

import com.example.mcpclient.model.SqlQueryRequest;
import com.example.mcpclient.model.SqlQueryResponse;
import com.example.mcpclient.service.SqlGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
@Tag(name = "SQL Generator", description = "API for generating SQL queries from natural language prompts using MCP and LLM")
public class SqlGeneratorController {

    private final SqlGeneratorService sqlGeneratorService;

    /**
     * Generates SQL query from natural language prompt
     * 
     * @param request Request containing the user's prompt
     * @return Response with generated SQL query and context
     */
    @Operation(
            summary = "Generate SQL query from natural language",
            description = "Accepts a natural language prompt, fetches database schema context from MCP server, " +
                    "and generates a SQL query using an LLM (GPT-4)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SQL query generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SqlQueryResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error generating SQL query",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SqlQueryResponse.class)
                    )
            )
    })
    @PostMapping("/generate")
    public ResponseEntity<SqlQueryResponse> generateSql(
            @Parameter(description = "Request containing the natural language prompt", required = true)
            @RequestBody SqlQueryRequest request) {
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
    @Operation(
            summary = "Health check",
            description = "Check if the MCP Client service is running"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy",
                    content = @Content(mediaType = "text/plain")
            )
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("MCP Client is running");
    }
}
