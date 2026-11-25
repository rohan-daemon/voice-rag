package com.voicerag.mcp;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dependency-free executable regression checks; run with assertions enabled. */
public class ObservedToolCallbackChecks {
    public static void main(String[] args) {
        List<McpToolCallEvent> events = new ArrayList<>();
        RuntimeException failure = new IllegalArgumentException("invalid request");
        ToolContext context = new ToolContext(Map.of("test", "context"));
        ToolCallback delegate = new ToolCallback() {
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("test").description("test")
                        .inputSchema("{}").build();
            }
            public String call(String input) {
                if (input.equals("fail")) throw failure;
                return "result";
            }
            public String call(String input, ToolContext supplied) {
                assert supplied == context : "Context was not preserved";
                return call(input);
            }
        };
        ObservedToolCallback callback = new ObservedToolCallback(delegate,
                event -> events.add((McpToolCallEvent) event));
        assert callback.getToolDefinition() == delegate.getToolDefinition() ||
                callback.getToolDefinition().name().equals("test");
        assert callback.call("request").equals("result");
        assert events.size() == 2;
        assert events.get(0).phase().equals("request");
        assert events.get(0).payload().equals("request");
        assert events.get(1).phase().equals("response");
        assert events.get(1).payload().equals("result");
        assert events.get(0).callId().equals(events.get(1).callId());
        assert events.get(1).durationMs() >= 0;
        String previousId = events.get(0).callId();
        events.clear();
        assert callback.call("context request", context).equals("result");
        assert !events.get(0).callId().equals(previousId);
        events.clear();
        try {
            callback.call("fail");
            throw new AssertionError("Expected failure");
        } catch (RuntimeException e) {
            assert e == failure : "Original exception was replaced";
        }
        assert events.size() == 2;
        assert events.get(1).phase().equals("error");
        assert events.get(1).error() == failure;
        ObservedToolCallback brokenObserver = new ObservedToolCallback(delegate,
                event -> { throw new IllegalStateException("observer unavailable"); });
        assert brokenObserver.call("request").equals("result");
        try {
            brokenObserver.call("fail");
            throw new AssertionError("Expected failure");
        } catch (RuntimeException e) {
            assert e == failure;
        }
        System.out.println("Observer checks passed: success, context, correlation, failure, observer isolation");
    }
}
