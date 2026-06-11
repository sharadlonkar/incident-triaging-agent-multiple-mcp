package com.walmart.incident.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class AgentConfig {

    /**
     * Builds a ChatClient wired with all tools from every MCP server.
     *
     * Spring AI MCP auto-configuration creates one ToolCallbackProvider per
     * connected SSE server. We flatten them into a single ToolCallback[] so
     * the ChatClient can invoke any tool across all 9 MCP servers in one call.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  List<ToolCallbackProvider> mcpToolProviders) {
        List<ToolCallback> allCallbacks = mcpToolProviders.stream()
            .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
            .collect(Collectors.toList());

        return builder
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultToolCallbacks(allCallbacks)
            .build();
    }
}
