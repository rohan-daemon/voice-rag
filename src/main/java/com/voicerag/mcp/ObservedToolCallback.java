package com.voicerag.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;
import java.util.function.Supplier;

/** Observes the callback boundary, including argument conversion and result serialization. */
public final class ObservedToolCallback implements ToolCallback {
    private static final Logger log = LoggerFactory.getLogger(ObservedToolCallback.class);
    private final ToolCallback delegate;
    private final ApplicationEventPublisher publisher;

    public ObservedToolCallback(ToolCallback delegate, ApplicationEventPublisher publisher) {
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String input) {
        return observe(input, () -> delegate.call(input));
    }

    @Override
    public String call(String input, ToolContext context) {
        return observe(input, () -> delegate.call(input, context));
    }

    private String observe(String input, Supplier<String> action) {
        String id = UUID.randomUUID().toString();
        long start = System.nanoTime();
        publish(new McpToolCallEvent(id, getToolDefinition().name(), "request", input, 0, null));
        try {
            String result = action.get();
            publish(new McpToolCallEvent(id, getToolDefinition().name(), "response", result,
                    (System.nanoTime() - start) / 1_000_000, null));
            return result;
        } catch (RuntimeException | Error error) {
            publish(new McpToolCallEvent(id, getToolDefinition().name(), "error", null,
                    (System.nanoTime() - start) / 1_000_000, error));
            throw error;
        }
    }

    private void publish(McpToolCallEvent event) {
        try {
            publisher.publishEvent(event);
        } catch (RuntimeException e) {
            // An observer failure must not change the tool's return value or exception.
            log.warn("Failed to publish MCP observation for callId={}", event.callId(), e);
        }
    }
}
