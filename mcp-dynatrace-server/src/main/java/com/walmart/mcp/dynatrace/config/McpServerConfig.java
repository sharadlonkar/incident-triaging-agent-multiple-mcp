package com.walmart.mcp.dynatrace.config;

import com.walmart.mcp.dynatrace.service.DynatraceService;
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
    public ToolCallbackProvider dynatraceTools(DynatraceService dynatraceService) {
        return MethodToolCallbackProvider.builder().toolObjects(dynatraceService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> dynatracePrompts() {
        var healthCheckPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("dynatrace-full-health-check",
                "Run a comprehensive Dynatrace health check for a service including Apdex, anomalies, and dependencies",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to analyze", true),
                         new McpSchema.PromptArgument("minutes", "Lookback minutes", true))),
            req -> new McpSchema.GetPromptResult("Full health check",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Run a full Dynatrace health check for '" + req.arguments().get("serviceName") +
                        "' over " + req.arguments().get("minutes") + " minutes. " +
                        "Include Apdex score analysis, all anomalies detected by Davis AI, " +
                        "dependency map health, and any open problems. Provide a severity assessment."
                    ))))
        );

        var rootCausePrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("dynatrace-root-cause-investigation",
                "Use Dynatrace Davis AI to perform root cause analysis for an active problem",
                List.of(new McpSchema.PromptArgument("problemId", "Dynatrace problem ID (P-DT-...)", true))),
            req -> new McpSchema.GetPromptResult("Root cause investigation",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Analyze Dynatrace problem '" + req.arguments().get("problemId") +
                        "' using Davis AI root cause analysis. " +
                        "Present the evidence chain, impacted entities, affected users, " +
                        "business impact, and prioritized remediation options."
                    ))))
        );

        var deploymentImpactPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("dynatrace-deployment-impact",
                "Analyze whether a recent deployment caused the current incident",
                List.of(new McpSchema.PromptArgument("serviceName", "Service name", true),
                         new McpSchema.PromptArgument("hours", "How many hours back to check", true))),
            req -> new McpSchema.GetPromptResult("Deployment impact analysis",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check if a recent deployment caused the current issue for '" +
                        req.arguments().get("serviceName") + "' by reviewing changes in the last " +
                        req.arguments().get("hours") + " hours. " +
                        "Correlate deployment timing with performance degradation onset. " +
                        "Recommend rollback if evidence is strong."
                    ))))
        );

        var anomalyIntelPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("dynatrace-anomaly-intelligence",
                "Get AI-powered anomaly intelligence and pattern recognition from Dynatrace Davis",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to inspect", true),
                         new McpSchema.PromptArgument("minutes", "Time window", true))),
            req -> new McpSchema.GetPromptResult("Anomaly intelligence",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Retrieve all Dynatrace Davis AI anomalies for '" + req.arguments().get("serviceName") +
                        "' in the last " + req.arguments().get("minutes") + " minutes. " +
                        "Rank by Davis confidence score, identify primary vs secondary anomalies, " +
                        "and determine the causal chain."
                    ))))
        );

        var cascadeAnalysisPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("dynatrace-cascade-analysis",
                "Map the cascade failure blast radius from a root cause service to all impacted services",
                List.of(new McpSchema.PromptArgument("rootService", "Suspected root cause service", true))),
            req -> new McpSchema.GetPromptResult("Cascade failure analysis",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Using Dynatrace dependency maps, trace the cascade failure starting from '" +
                        req.arguments().get("rootService") + "'. " +
                        "Identify all impacted downstream services, quantify degradation at each hop, " +
                        "and estimate total blast radius (users, revenue, SLO impact)."
                    ))))
        );

        return List.of(healthCheckPrompt, rootCausePrompt, deploymentImpactPrompt, anomalyIntelPrompt, cascadeAnalysisPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> dynatraceResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("dynatrace://resources/tools-description",
                "Dynatrace Tools Description",
                "Complete description of all Dynatrace MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("dynatrace://resources/tools-description", "text/plain",
                    """
                    Dynatrace APM MCP Server Tools
                    ===============================

                    Provides AI-powered observability from Dynatrace OneAgent and Davis AI.

                    AVAILABLE TOOLS:

                    1. getApplicationHealth(serviceName, minutes)
                       - Apdex score (0-1), health score (0-100), availability %
                       - Throughput vs baseline comparison
                       - Davis AI status and insight summary
                       - Key indicator for executive-level service health

                    2. getAnomalies(serviceName, minutes)
                       - Davis AI-detected anomalies with confidence scores
                       - Types: response time, failure rate, throughput, memory, CPU
                       - Evidence for each anomaly with baselining data
                       - Distinguishes primary cause from secondary effects

                    3. getDependencyMap(serviceName)
                       - Full upstream/downstream service dependency graph
                       - Health status of each dependency
                       - Identifies which dependency is the root cause
                       - Critical for blast radius assessment

                    4. getRootCauseAnalysis(problemId)
                       - Davis AI root cause with evidence chain
                       - Affected user count and business impact estimate
                       - Prioritized remediation options with time estimates
                       - Problem timeline and duration

                    5. getDeploymentImpact(serviceName, hours)
                       - Recent deployments detected by OneAgent
                       - Change correlation with performance onset
                       - Risk score per deployment
                       - Rollback recommendation with confidence level

                    BEST PRACTICES:
                    - Use getApplicationHealth first for overall status
                    - Follow with getAnomalies for detailed evidence
                    - Use getRootCauseAnalysis for Davis AI's conclusion
                    - Cross-reference getDeploymentImpact for change-caused incidents
                    """)
            ))
        );

        var problemsIndex = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("dynatrace://resources/active-problems",
                "Active Dynatrace Problems Index",
                "Index of all currently open Dynatrace problems across Walmart services",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("dynatrace://resources/active-problems", "application/json",
                    """
                    {
                      "timestamp": "2026-06-11T16:30:00Z",
                      "openProblems": [
                        {
                          "problemId": "P-DT-20240611-4421",
                          "title": "order-mgmt — Availability drop 65.3%%",
                          "severity": "AVAILABILITY",
                          "status": "OPEN",
                          "openedAt": "2026-06-11T14:23:00Z",
                          "duration": "2h 7m",
                          "impactedServices": 2,
                          "affectedUsers": 84320,
                          "rootCause": "DB connection pool exhaustion"
                        },
                        {
                          "problemId": "P-DT-20240611-4422",
                          "title": "fulfillment-mgmt — Cascade degradation",
                          "severity": "PERFORMANCE",
                          "status": "OPEN",
                          "openedAt": "2026-06-11T14:26:00Z",
                          "duration": "2h 4m",
                          "impactedServices": 1,
                          "linkedTo": "P-DT-20240611-4421"
                        }
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, problemsIndex);
    }
}
