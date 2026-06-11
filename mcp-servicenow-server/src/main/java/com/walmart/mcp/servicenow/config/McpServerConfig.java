package com.walmart.mcp.servicenow.config;

import com.walmart.mcp.servicenow.service.ServiceNowService;
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
    public ToolCallbackProvider serviceNowTools(ServiceNowService serviceNowService) {
        return MethodToolCallbackProvider.builder().toolObjects(serviceNowService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> serviceNowPrompts() {
        var incidentContextPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("servicenow-incident-context",
                "Retrieve full incident context from ServiceNow including timeline and work notes",
                List.of(new McpSchema.PromptArgument("serviceName", "Affected service name", true))),
            req -> new McpSchema.GetPromptResult("ServiceNow incident context",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Get all active ServiceNow incidents for '" + req.arguments().get("serviceName") +
                        "'. For each incident: retrieve full details including work notes, SLA status, " +
                        "and related change requests. Build a complete picture of the formal incident record."
                    ))))
        );

        var changeCorrelationPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("servicenow-change-correlation",
                "Correlate recent change requests with the current incident to identify change-caused failures",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to check changes for", true),
                         new McpSchema.PromptArgument("hours", "How many hours back to look", true))),
            req -> new McpSchema.GetPromptResult("Change correlation",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Get recent change requests for '" + req.arguments().get("serviceName") +
                        "' in the last " + req.arguments().get("hours") + " hours. " +
                        "Determine if any change (deployment, config, infrastructure) could have caused the current incident. " +
                        "Highlight config changes that differ from previous stable versions."
                    ))))
        );

        var slaStatusPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("servicenow-sla-assessment",
                "Check if SLA deadlines are at risk for active incidents",
                List.of(new McpSchema.PromptArgument("incidentNumber", "ServiceNow incident number", true))),
            req -> new McpSchema.GetPromptResult("SLA assessment",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check SLA status for incident '" + req.arguments().get("incidentNumber") +
                        "'. Determine: time remaining before SLA breach, whether escalation is needed, " +
                        "and recommend immediate actions to meet the resolution deadline."
                    ))))
        );

        var knownErrorPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("servicenow-known-error-lookup",
                "Search ServiceNow KEDB (Known Error Database) for similar past incidents and their resolutions",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to look up", true))),
            req -> new McpSchema.GetPromptResult("Known error lookup",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Search ServiceNow problem records and known error database for '" +
                        req.arguments().get("serviceName") + "'. " +
                        "Find any prior occurrences of similar issues and their documented resolutions. " +
                        "This can significantly accelerate root cause identification."
                    ))))
        );

        var incidentReportPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("servicenow-incident-report",
                "Generate a formal incident report from ServiceNow data for post-incident review",
                List.of(new McpSchema.PromptArgument("incidentNumber", "Incident number", true))),
            req -> new McpSchema.GetPromptResult("Incident report",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Generate a formal post-incident report for '" + req.arguments().get("incidentNumber") +
                        "' using ServiceNow data. Include: incident timeline, root cause, business impact, " +
                        "actions taken, resolution method, and recommendations to prevent recurrence."
                    ))))
        );

        return List.of(incidentContextPrompt, changeCorrelationPrompt, slaStatusPrompt, knownErrorPrompt, incidentReportPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> serviceNowResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("servicenow://resources/tools-description",
                "ServiceNow Tools Description",
                "Description of all ServiceNow ITSM MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("servicenow://resources/tools-description", "text/plain",
                    """
                    ServiceNow ITSM MCP Server Tools
                    =================================

                    Provides access to Walmart's ServiceNow instance for incident management,
                    change tracking, and problem records.

                    AVAILABLE TOOLS:

                    1. getIncidents(serviceName, status)
                       - Active incidents for a service with work notes
                       - Full description written by engineers
                       - Timeline of updates and findings
                       - SLA tracking and escalation level
                       - Status: NEW, IN_PROGRESS, RESOLVED, ALL

                    2. getIncidentDetails(incidentNumber)
                       - Complete incident detail with full timeline
                       - Related changes and linked incidents
                       - Customer impact and revenue estimates
                       - SLA status and deadline

                    3. getChangeRequests(serviceName, hours)
                       - Recent deployments and config changes
                       - Before/after values for configuration parameters
                       - Risk level and rollback plan
                       - Links to related incidents (post-deployment)

                    4. getProblemRecords(serviceName)
                       - Root cause analysis records
                       - Known Error Database (KEDB) entries
                       - Prior incidents with same pattern
                       - Permanent fix proposals

                    5. createIncident(title, description, severity, serviceName)
                       - Create a new incident record
                       - Auto-assigns to service team
                       - Triggers PagerDuty for severity 1/2
                       - Returns incident number for tracking

                    SEVERITY LEVELS: 1-Critical, 2-High, 3-Medium, 4-Low
                    SLA: P1=4hr resolution, P2=8hr, P3=24hr, P4=72hr
                    """)
            ))
        );

        var severityMatrix = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("servicenow://resources/severity-matrix",
                "Incident Severity and Priority Matrix",
                "Walmart's incident severity classification matrix",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("servicenow://resources/severity-matrix", "application/json",
                    """
                    {
                      "severityMatrix": [
                        {
                          "severity": "1 - Critical (P1)",
                          "criteria": "Complete service outage OR >10%% error rate on Tier-1 service",
                          "businessImpact": "Customer-facing, revenue loss >$500k/hr",
                          "responseSLA": "15 minutes",
                          "resolutionSLA": "4 hours",
                          "escalation": "VP Engineering notified within 30 minutes"
                        },
                        {
                          "severity": "2 - High (P2)",
                          "criteria": ">5%% error rate OR significant performance degradation on Tier-1 service",
                          "businessImpact": "Customer-facing, revenue loss $100k-500k/hr",
                          "responseSLA": "30 minutes",
                          "resolutionSLA": "8 hours"
                        },
                        {
                          "severity": "3 - Medium (P3)",
                          "criteria": "Non-critical service degradation, internal tooling impacted",
                          "responseSLA": "2 hours",
                          "resolutionSLA": "24 hours"
                        },
                        {
                          "severity": "4 - Low (P4)",
                          "criteria": "Minor issue, no immediate customer impact",
                          "responseSLA": "8 hours",
                          "resolutionSLA": "72 hours"
                        }
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, severityMatrix);
    }
}
