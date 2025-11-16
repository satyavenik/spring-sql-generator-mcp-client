package com.example.mcpclient.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model containing the generated SQL query and context")
public class SqlQueryResponse {

    @Schema(
            description = "The generated SQL query",
            example = "SELECT * FROM users WHERE registration_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)"
    )
    private String sqlQuery;

    @Schema(
            description = "Database schema context retrieved from the MCP server",
            example = "Database schema: users table with columns: id, name, email, registration_date"
    )
    private String context;
}
