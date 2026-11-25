package com.voicerag.mcp;

/** A tool execution event. LLM token usage is not available at the MCP server. */
public record McpToolCallEvent(String callId, String tool, String phase,
                               String payload, long durationMs, Throwable error) {
}
