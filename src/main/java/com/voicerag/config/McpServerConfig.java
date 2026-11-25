package com.voicerag.config;

import com.voicerag.mcp.VoiceRagMcpTools;
import com.voicerag.mcp.ObservedToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Arrays;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider voiceRagToolCallbackProvider(VoiceRagMcpTools voiceRagMcpTools,
                                                          ApplicationEventPublisher publisher) {
        ToolCallbackProvider delegate = MethodToolCallbackProvider.builder()
                .toolObjects(voiceRagMcpTools)
                .build();
        return () -> Arrays.stream(delegate.getToolCallbacks())
                .map(callback -> new ObservedToolCallback(callback, publisher))
                .toArray(ToolCallback[]::new);
    }
}
