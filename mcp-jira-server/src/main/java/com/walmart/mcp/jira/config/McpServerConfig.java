package com.walmart.mcp.jira.config;

import com.walmart.mcp.jira.service.JiraService;
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
    public ToolCallbackProvider jiraTools(JiraService jiraService) {
        return MethodToolCallbackProvider.builder().toolObjects(jiraService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> jiraPrompts() {
        var knownBugCheckPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("jira-known-bug-check",
                "Check Jira for known bugs that might be related to the current incident",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to check bugs for", true))),
            req -> new McpSchema.GetPromptResult("Known bug check",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Search Jira for open bugs in '" + req.arguments().get("serviceName") +
                        "'. Determine if any known bugs match the current incident symptoms. " +
                        "This can accelerate root cause identification by matching to documented known issues."
                    ))))
        );

        var sprintCapacityPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("jira-sprint-capacity",
                "Check team sprint capacity and workload during the incident to understand available resources",
                List.of(new McpSchema.PromptArgument("teamName", "Engineering team", true))),
            req -> new McpSchema.GetPromptResult("Sprint capacity",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check current sprint status for '" + req.arguments().get("teamName") +
                        "'. How many engineers are available? What were they working on before the incident? " +
                        "Are there related in-progress items that might be connected to the incident?"
                    ))))
        );

        var orderImpactPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("jira-order-impact",
                "Find all Jira issues linked to a specific order to understand customer impact",
                List.of(new McpSchema.PromptArgument("orderId", "Order ID to look up", true))),
            req -> new McpSchema.GetPromptResult("Order impact",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Find all Jira issues linked to order '" + req.arguments().get("orderId") +
                        "'. Determine the specific customer impact, any escalations raised, " +
                        "and whether payment pre-authorization is at risk of expiring."
                    ))))
        );

        var incidentBugCreatePrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("jira-create-incident-bug",
                "Create a bug ticket in Jira to track a root cause finding from the incident",
                List.of(new McpSchema.PromptArgument("finding", "Root cause finding to document", true),
                         new McpSchema.PromptArgument("serviceName", "Affected service", true))),
            req -> new McpSchema.GetPromptResult("Create bug",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Create a Jira bug ticket to track this root cause finding: '" +
                        req.arguments().get("finding") + "' for service '" +
                        req.arguments().get("serviceName") + "'. " +
                        "Include: what the bug is, how to reproduce, impact, and recommended fix."
                    ))))
        );

        var recentChangesPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("jira-recent-work",
                "Review recent Jira work items for a service to find technical debt or changes that could cause issues",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to review", true),
                         new McpSchema.PromptArgument("days", "Lookback in days", true))),
            req -> new McpSchema.GetPromptResult("Recent work review",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Review recent Jira issues for '" + req.arguments().get("serviceName") +
                        "' in the last " + req.arguments().get("days") + " days. " +
                        "Identify: technical debt items that may have contributed to the incident, " +
                        "work items that should have been completed before the problematic deployment, " +
                        "and items that could accelerate recovery."
                    ))))
        );

        return List.of(knownBugCheckPrompt, sprintCapacityPrompt, orderImpactPrompt, incidentBugCreatePrompt, recentChangesPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> jiraResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("jira://resources/tools-description",
                "Jira Tools Description",
                "Description of all Jira issue tracking MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("jira://resources/tools-description", "text/plain",
                    """
                    Jira Issue Tracking MCP Server Tools
                    =====================================

                    Provides access to Walmart's Jira instance for issue tracking,
                    sprint management, and customer escalation tracking.

                    AVAILABLE TOOLS:

                    1. getOpenBugs(serviceName)
                       - All open bug reports for a service
                       - Prioritized by severity and business impact
                       - Links to incidents and PRs if related
                       - Useful for matching incident to known issues

                    2. getRecentIssues(serviceName, days)
                       - All recent issues (bugs, tasks, stories, tech debt)
                       - Identifies work done before the incident that may be related
                       - Shows unfinished items that could have prevented the incident

                    3. getSprintStatus(teamName)
                       - Current sprint velocity and capacity
                       - Team availability for incident response
                       - In-progress items that may be connected to the incident

                    4. getLinkedIssues(orderId)
                       - All Jira issues linked to a specific order
                       - Customer escalations and payment pre-auth expiry
                       - Recommended customer communication actions

                    5. createBug(title, description, serviceName, priority)
                       - Create a bug ticket from incident findings
                       - Auto-assigns to correct project based on service
                       - Returns Jira issue key for tracking

                    PROJECTS: ORDER, FULFILL, DLVRY, PLATFORM
                    PRIORITIES: Blocker > Critical > High > Medium > Low
                    """)
            ))
        );

        var projectIndex = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("jira://resources/project-index",
                "Jira Project to Service Mapping",
                "Maps services to their Jira project keys and team boards",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("jira://resources/project-index", "application/json",
                    """
                    {
                      "projects": [
                        {
                          "key": "ORDER",
                          "name": "Order Management Platform",
                          "services": ["order-mgmt", "payment-service", "notification-service"],
                          "team": "Order Platform",
                          "board": "Order Platform Sprint Board"
                        },
                        {
                          "key": "FULFILL",
                          "name": "Fulfillment Platform",
                          "services": ["fulfillment-mgmt", "pre-pick-mgmt", "pick-mgmt", "post-pick-mgmt", "warehouse-service"],
                          "team": "Fulfillment Platform"
                        },
                        {
                          "key": "DLVRY",
                          "name": "Last Mile Delivery",
                          "services": ["delivery-mgmt", "route-optimizer", "driver-service"],
                          "team": "Last Mile"
                        },
                        {
                          "key": "PLATFORM",
                          "name": "Infrastructure Platform",
                          "services": ["api-gateway", "auth-service", "inventory-service"],
                          "team": "Infrastructure SRE"
                        }
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, projectIndex);
    }
}
