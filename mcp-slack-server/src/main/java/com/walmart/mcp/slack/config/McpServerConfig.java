package com.walmart.mcp.slack.config;

import com.walmart.mcp.slack.service.SlackService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider slackTools(SlackService slackService) {
        return MethodToolCallbackProvider.builder().toolObjects(slackService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> slackPrompts() {
        var incidentContextPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("slack-incident-context",
                "Gather human context about the incident from Slack discussions",
                List.of(new McpSchema.PromptArgument("minutes", "How far back to look in minutes", true))),
            req -> new McpSchema.GetPromptResult("Incident context from Slack",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Read the #incidents-p1 channel for the last " + req.arguments().get("minutes") +
                        " minutes to understand what the engineering team has already discovered. " +
                        "Summarize key findings, who is working on the incident, current status, " +
                        "and what actions are already in progress. Avoid duplicating investigation already done."
                    ))))
        );

        var humanIntelPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("slack-human-intelligence",
                "Extract human-reported facts and hypotheses from all incident-related Slack discussions",
                List.of(new McpSchema.PromptArgument("keyword", "Search keyword (service name or error)", true),
                         new McpSchema.PromptArgument("minutes", "Lookback window", true))),
            req -> new McpSchema.GetPromptResult("Human intelligence from Slack",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Search Slack for '" + req.arguments().get("keyword") +
                        "' across all channels in the last " + req.arguments().get("minutes") + " minutes. " +
                        "Extract: (1) facts already confirmed by engineers, (2) active hypotheses being investigated, " +
                        "(3) actions already taken, (4) who the key responders are."
                    ))))
        );

        var oncallContactPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("slack-get-oncall-contacts",
                "Find the current on-call engineers for impacted services",
                List.of(new McpSchema.PromptArgument("serviceName", "Service name", true))),
            req -> new McpSchema.GetPromptResult("On-call contacts",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Get the current on-call engineer and backup for '" + req.arguments().get("serviceName") +
                        "'. Include their Slack handle, phone number, and current status (acknowledged/available)."
                    ))))
        );

        var warRoomPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("slack-war-room-status",
                "Get the current state of all active incident war rooms",
                List.of()),
            req -> new McpSchema.GetPromptResult("War room status",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check all active incident discussions in Slack. " +
                        "For each active incident: provide current status, key findings, actions in progress, " +
                        "and estimated time to resolution. Flag any P1 incidents immediately."
                    ))))
        );

        var summaryPostPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("slack-post-executive-summary",
                "Format and post an executive incident summary to appropriate Slack channels",
                List.of(new McpSchema.PromptArgument("summary", "Executive summary to post", true),
                         new McpSchema.PromptArgument("channel", "Target Slack channel", true))),
            req -> new McpSchema.GetPromptResult("Post summary",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Post the following executive summary to Slack channel '" +
                        req.arguments().get("channel") + "': " + req.arguments().get("summary") +
                        "\n\nFormat it for Slack markdown with appropriate urgency indicators."
                    ))))
        );

        return List.of(incidentContextPrompt, humanIntelPrompt, oncallContactPrompt, warRoomPrompt, summaryPostPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> slackResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("slack://resources/tools-description",
                "Slack Tools Description",
                "Description of all Slack communication MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("slack://resources/tools-description", "text/plain",
                    """
                    Slack Team Communication MCP Server Tools
                    ==========================================

                    Provides access to Walmart's Slack workspace for incident response
                    team communication, context gathering, and status updates.

                    AVAILABLE TOOLS:

                    1. getChannelMessages(channel, minutes)
                       - Messages from a specific channel with user context
                       - Shows what engineers have discovered and discussed
                       - Includes bot alerts and human responses
                       - Key channels: #incidents-p1, #order-platform-alerts, #fulfillment-platform-alerts

                    2. searchMessages(keyword, minutes)
                       - Cross-channel keyword search
                       - Returns results grouped by channel
                       - Useful for finding all discussions about a specific service or error

                    3. getOnCallTeam(serviceName)
                       - Current on-call engineer details per service
                       - Primary and secondary contacts with Slack handles
                       - Escalation path and manager contacts
                       - Status: acknowledged/available/unreachable

                    4. getActiveIncidentDiscussions()
                       - All currently open incident war rooms
                       - Incident summary, duration, participant count
                       - Key timeline of updates for each incident
                       - Current status and next action

                    5. postMessage(channel, message)
                       - Send a message to a Slack channel
                       - Used to publish executive summaries and status updates
                       - Returns message timestamp for threading

                    IMPORTANT: Slack provides human context that metrics tools cannot.
                    Engineers in #incidents-p1 often know the root cause before metrics confirm it.
                    Always check Slack first to avoid duplicating investigation.
                    """)
            ))
        );

        var channelDirectory = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("slack://resources/channel-directory",
                "Slack Channel Directory",
                "List of important Slack channels for incident response",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("slack://resources/channel-directory", "application/json",
                    """
                    {
                      "incidentChannels": [
                        { "channel": "#incidents-p1", "purpose": "All P1 production incidents", "members": 124 },
                        { "channel": "#incidents-p2", "purpose": "P2 incidents and degradations", "members": 89 }
                      ],
                      "serviceChannels": [
                        { "channel": "#order-platform-alerts", "purpose": "order-mgmt alerts and discussions", "team": "Order Platform" },
                        { "channel": "#fulfillment-platform-alerts", "purpose": "fulfillment service alerts", "team": "Fulfillment Platform" },
                        { "channel": "#last-mile-alerts", "purpose": "delivery-mgmt alerts", "team": "Last Mile" }
                      ],
                      "leadershipChannels": [
                        { "channel": "#engineering-leadership", "purpose": "VP/Director incident escalations" },
                        { "channel": "#engineering-oncall", "purpose": "On-call handoffs and escalations" }
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, channelDirectory);
    }
}
