package com.walmart.mcp.pagerduty.config;

import com.walmart.mcp.pagerduty.service.PagerDutyService;
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
    public ToolCallbackProvider pagerDutyTools(PagerDutyService pagerDutyService) {
        return MethodToolCallbackProvider.builder().toolObjects(pagerDutyService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> pagerDutyPrompts() {
        var alertStatusPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("pagerduty-alert-status",
                "Get comprehensive alert status for a service showing all triggered and acknowledged alerts",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to check alerts for", true))),
            req -> new McpSchema.GetPromptResult("Alert status",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Get all active PagerDuty alerts for '" + req.arguments().get("serviceName") +
                        "'. Summarize what's currently firing, who has acknowledged what, " +
                        "and which alerts are still unacknowledged (potentially missed)."
                    ))))
        );

        var responderStatusPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("pagerduty-responder-status",
                "Check who is currently responding and if anyone needs to be paged or escalated",
                List.of(new McpSchema.PromptArgument("teamName", "Engineering team name", true))),
            req -> new McpSchema.GetPromptResult("Responder status",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check on-call schedule for '" + req.arguments().get("teamName") +
                        "'. Is the primary on-call responding? Do we need to escalate to secondary or management? " +
                        "Provide escalation path and contact details."
                    ))))
        );

        var incidentTimelinePrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("pagerduty-incident-timeline",
                "Build a comprehensive incident response timeline from PagerDuty events",
                List.of(new McpSchema.PromptArgument("incidentId", "PagerDuty or ServiceNow incident ID", true))),
            req -> new McpSchema.GetPromptResult("Incident timeline",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Get the full PagerDuty timeline for incident '" + req.arguments().get("incidentId") +
                        "'. Include: when alerts fired, who was paged, MTTA, escalations, and current status. " +
                        "Calculate time from first alert to current state and assess if response is fast enough."
                    ))))
        );

        var reliabilityReportPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("pagerduty-reliability-report",
                "Generate a reliability report with MTTA/MTTR trends to contextualize the current incident",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to report on", true))),
            req -> new McpSchema.GetPromptResult("Reliability report",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Generate a 30-day reliability report for '" + req.arguments().get("serviceName") +
                        "'. Include MTTA, MTTR, incident frequency by severity, alert noise ratio, " +
                        "and compare current incident severity to historical patterns."
                    ))))
        );

        var escalationPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("pagerduty-escalation-assessment",
                "Assess whether the current incident requires escalation to senior leadership",
                List.of(new McpSchema.PromptArgument("incidentId", "Incident to assess", true),
                         new McpSchema.PromptArgument("durationMinutes", "How long the incident has been open", true))),
            req -> new McpSchema.GetPromptResult("Escalation assessment",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Assess whether incident '" + req.arguments().get("incidentId") +
                        "' (open for " + req.arguments().get("durationMinutes") + " minutes) requires " +
                        "escalation to VP or C-level. Consider: duration, revenue impact, customer count, " +
                        "SLA breach risk, and whether resolution is in sight."
                    ))))
        );

        return List.of(alertStatusPrompt, responderStatusPrompt, incidentTimelinePrompt, reliabilityReportPrompt, escalationPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> pagerDutyResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("pagerduty://resources/tools-description",
                "PagerDuty Tools Description",
                "Description of all PagerDuty/xMatters alerting MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("pagerduty://resources/tools-description", "text/plain",
                    """
                    PagerDuty/xMatters Alerting MCP Server Tools
                    =============================================

                    Provides incident alerting, on-call management, and response tracking
                    for Walmart's engineering teams.

                    AVAILABLE TOOLS:

                    1. getActiveAlerts(serviceName)
                       - All triggered and acknowledged alerts for a service
                       - Alert details: severity, duration, who acknowledged
                       - Identifies unacknowledged alerts (missed pages)
                       - Links to ServiceNow incidents

                    2. getOnCallSchedule(teamName)
                       - Current primary and secondary on-call contact
                       - Escalation policy (L1 → L2 → Manager → VP)
                       - Contact methods (SMS, app, email)
                       - On-call rotation schedule

                    3. getIncidentTimeline(incidentId)
                       - Chronological PagerDuty events for an incident
                       - Alert trigger times, page times, ack times
                       - MTTA measurement and escalation events
                       - Current status in response lifecycle

                    4. getNotificationLog(incidentId)
                       - All xMatters notifications sent for an incident
                       - Delivery status and response times per responder
                       - Escalation chain evidence

                    5. getServiceReliabilityMetrics(serviceName)
                       - 30-day MTTA and MTTR statistics
                       - Incident frequency by severity
                       - Alert noise ratio (actionable vs noise)
                       - SLA compliance percentage

                    SEVERITY: critical > error > warning > info
                    MTTA TARGET: <15 minutes for P1
                    MTTR TARGET: <4 hours for P1
                    """)
            ))
        );

        var teamDirectory = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("pagerduty://resources/team-directory",
                "Engineering Team Directory and On-Call Schedules",
                "All engineering teams with their PagerDuty service mappings",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("pagerduty://resources/team-directory", "application/json",
                    """
                    {
                      "teams": [
                        {
                          "name": "Order Platform SRE",
                          "services": ["order-mgmt", "payment-service", "notification-service"],
                          "pdTeamId": "PT-ORDER-SRE",
                          "slackChannel": "#order-platform-alerts",
                          "oncallRotation": "weekly"
                        },
                        {
                          "name": "Fulfillment Platform",
                          "services": ["fulfillment-mgmt", "pre-pick-mgmt", "pick-mgmt", "post-pick-mgmt"],
                          "pdTeamId": "PT-FULFILL",
                          "slackChannel": "#fulfillment-platform-alerts"
                        },
                        {
                          "name": "Last Mile",
                          "services": ["delivery-mgmt", "route-optimizer"],
                          "pdTeamId": "PT-LASTMILE",
                          "slackChannel": "#last-mile-alerts"
                        },
                        {
                          "name": "Infrastructure SRE",
                          "services": ["api-gateway", "auth-service", "kubernetes", "database"],
                          "pdTeamId": "PT-INFRA-SRE",
                          "slackChannel": "#infrastructure-alerts"
                        }
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, teamDirectory);
    }
}
