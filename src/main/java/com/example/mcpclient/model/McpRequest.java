package com.example.mcpclient.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    private String jsonrpc;
    private String method;
    private Object params;
    private String id;
}
