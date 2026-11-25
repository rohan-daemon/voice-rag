package com.voicerag.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class McpToolCallObserver {
    private static final Logger log = LoggerFactory.getLogger(McpToolCallObserver.class);
    private final ObjectMapper mapper;

    public McpToolCallObserver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @EventListener
    public void onToolCall(McpToolCallEvent event) {
        // Encode payloads as JSON strings so embedded newlines cannot forge log lines.
        String payload;
        try {
            payload = mapper.writeValueAsString(event.payload());
        } catch (JsonProcessingException e) {
            payload = "\"<payload serialization failed>\"";
        }
        if (event.error() == null) {
            log.info("MCP callId={} tool={} phase={} durationMs={} tokens=unavailable payload={}",
                    event.callId(), event.tool(), event.phase(), event.durationMs(), payload);
        } else {
            log.error("MCP callId={} tool={} phase={} durationMs={} tokens=unavailable payload={}",
                    event.callId(), event.tool(), event.phase(), event.durationMs(), payload, event.error());
        }
    }
}
