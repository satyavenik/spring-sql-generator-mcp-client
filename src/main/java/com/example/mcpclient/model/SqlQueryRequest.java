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
@Schema(description = "Request model for SQL query generation")
public class SqlQueryRequest {

    @Schema(
            description = "Natural language prompt describing the desired SQL query",
            example = "Get all users who registered in the last 30 days",
            required = true
    )
    private String prompt;
}
