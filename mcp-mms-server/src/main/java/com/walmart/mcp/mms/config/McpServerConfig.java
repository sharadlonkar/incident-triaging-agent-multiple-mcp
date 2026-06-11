package com.walmart.mcp.mms.config;

import com.walmart.mcp.mms.service.MmsService;
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
    public ToolCallbackProvider mmsTools(MmsService mmsService) {
        return MethodToolCallbackProvider.builder().toolObjects(mmsService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> mmsPrompts() {
        var analyzeLatencyPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("analyze-service-latency",
                "Analyze latency degradation pattern for a microservice and identify root cause",
                List.of(
                    new McpSchema.PromptArgument("serviceName", "Name of the service (e.g. order-mgmt)", true),
                    new McpSchema.PromptArgument("minutes", "Lookback window in minutes", true)
                )),
            req -> new McpSchema.GetPromptResult("Analyze service latency",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Analyze the latency metrics for service '" + req.arguments().get("serviceName") +
                        "' over the last " + req.arguments().get("minutes") + " minutes. " +
                        "Identify if there is degradation, the severity compared to baseline, and likely root cause. " +
                        "Correlate with error rates and DB metrics if available."
                    ))))
        );

        var dbHealthPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("assess-database-health",
                "Assess database health and connection pool status for a service's database",
                List.of(
                    new McpSchema.PromptArgument("dbInstance", "Database instance name", true),
                    new McpSchema.PromptArgument("minutes", "Lookback window in minutes", true)
                )),
            req -> new McpSchema.GetPromptResult("Assess DB health",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Assess the health of database '" + req.arguments().get("dbInstance") +
                        "' over the last " + req.arguments().get("minutes") + " minutes. " +
                        "Check connection pool utilization, slow queries, deadlocks, and replication lag. " +
                        "Determine if DB issues are a contributing cause to service degradation."
                    ))))
        );

        var alertTriagePrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("triage-active-alerts",
                "Triage all active alerts and prioritize by business impact",
                List.of(
                    new McpSchema.PromptArgument("severity", "Minimum severity: CRITICAL, WARNING, or INFO", false)
                )),
            req -> new McpSchema.GetPromptResult("Triage alerts",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Retrieve all active alerts at severity '" + req.arguments().getOrDefault("severity", "CRITICAL") +
                        "' or higher. Group by service, identify cascading failures, and provide a prioritized " +
                        "remediation order based on business impact."
                    ))))
        );

        var infraCapacityPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("check-infra-capacity",
                "Check if infrastructure resource exhaustion is contributing to service degradation",
                List.of(
                    new McpSchema.PromptArgument("hostName", "Kubernetes node hostname", true),
                    new McpSchema.PromptArgument("minutes", "Lookback window in minutes", true)
                )),
            req -> new McpSchema.GetPromptResult("Check infra capacity",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check infrastructure metrics for host '" + req.arguments().get("hostName") +
                        "' over " + req.arguments().get("minutes") + " minutes. " +
                        "Assess CPU, memory, disk I/O, and network saturation. " +
                        "Determine if resource exhaustion is causing or worsening the incident."
                    ))))
        );

        var errorPatternPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("analyze-error-patterns",
                "Analyze error rate patterns to identify affected endpoints and classify failure types",
                List.of(
                    new McpSchema.PromptArgument("serviceName", "Service name", true),
                    new McpSchema.PromptArgument("minutes", "Lookback window in minutes", true)
                )),
            req -> new McpSchema.GetPromptResult("Analyze error patterns",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Analyze error patterns for '" + req.arguments().get("serviceName") +
                        "' over " + req.arguments().get("minutes") + " minutes. " +
                        "Break down by HTTP status codes, identify the most impacted endpoints, " +
                        "and determine if this is infrastructure, application, or dependency failure."
                    ))))
        );

        return List.of(analyzeLatencyPrompt, dbHealthPrompt, alertTriagePrompt, infraCapacityPrompt, errorPatternPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> mmsResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("mms://resources/tools-description",
                "MMS Tools Description",
                "Complete description of all MMS tools and their use cases for incident triaging",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("mms://resources/tools-description", "text/plain",
                    """
                    MMS (Metrics Management System) - Prometheus/Grafana MCP Server Tools
                    ======================================================================

                    This MCP server provides metrics from the Walmart Metrics Management System,
                    powered by Prometheus time-series data and Grafana dashboards.

                    AVAILABLE TOOLS:

                    1. getServiceLatency(serviceName, minutes)
                       - Retrieves p50/p90/p99 latency percentiles for a microservice
                       - Includes baseline comparison, request rate, and trend direction
                       - Supported services: order-mgmt, fulfillment-mgmt, pre-pick-mgmt,
                         pick-mgmt, post-pick-mgmt, delivery-mgmt

                    2. getErrorRate(serviceName, minutes)
                       - Returns HTTP error breakdown (4xx, 5xx, timeouts) with percentages
                       - Lists most affected endpoints and provides root cause indicators
                       - Useful for quantifying customer impact

                    3. getDatabaseMetrics(dbInstance, minutes)
                       - HikariCP connection pool stats (active, idle, pending, timeouts)
                       - Slow query counts, deadlock counts, replication lag
                       - Critical for diagnosing DB-related performance issues

                    4. getInfraMetrics(hostName, minutes)
                       - Node-level CPU, memory, disk I/O, and network metrics
                       - Identifies resource saturation and OOM conditions
                       - Maps hosts to service workloads

                    5. getActiveAlerts(severity, serviceName)
                       - All firing Prometheus alerts filtered by severity and/or service
                       - Includes alert duration, labels, and descriptive messages
                       - Use to get a quick incident overview

                    USAGE GUIDANCE:
                    - Start with getActiveAlerts to get a holistic view
                    - Drill into specific services with getServiceLatency and getErrorRate
                    - For DB issues, call getDatabaseMetrics with the service's DB instance
                    - Cross-reference with getInfraMetrics for resource exhaustion scenarios
                    """)
            ))
        );

        var serviceInventory = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("mms://resources/service-inventory",
                "Service Inventory and SLO Thresholds",
                "List of all monitored services with their SLO thresholds and DB mappings",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("mms://resources/service-inventory", "application/json",
                    """
                    {
                      "services": [
                        {
                          "name": "order-mgmt",
                          "team": "order-platform",
                          "tier": 1,
                          "slo": { "p99_latency_ms": 500, "error_rate_pct": 1.0, "availability_pct": 99.9 },
                          "database": "order-mgmt-db",
                          "namespace": "order-platform",
                          "ports": [8080, 9090]
                        },
                        {
                          "name": "fulfillment-mgmt",
                          "team": "fulfillment-platform",
                          "tier": 1,
                          "slo": { "p99_latency_ms": 800, "error_rate_pct": 1.0, "availability_pct": 99.9 },
                          "database": "fulfillment-mgmt-db",
                          "namespace": "fulfillment-platform"
                        },
                        {
                          "name": "pre-pick-mgmt",
                          "team": "fulfillment-platform",
                          "tier": 2,
                          "slo": { "p99_latency_ms": 1000, "error_rate_pct": 2.0, "availability_pct": 99.5 },
                          "database": "pick-mgmt-db",
                          "namespace": "fulfillment-platform"
                        },
                        {
                          "name": "pick-mgmt",
                          "team": "fulfillment-platform",
                          "tier": 2,
                          "slo": { "p99_latency_ms": 1000, "error_rate_pct": 2.0, "availability_pct": 99.5 },
                          "database": "pick-mgmt-db",
                          "namespace": "fulfillment-platform"
                        },
                        {
                          "name": "post-pick-mgmt",
                          "team": "fulfillment-platform",
                          "tier": 2,
                          "slo": { "p99_latency_ms": 1000, "error_rate_pct": 2.0, "availability_pct": 99.5 },
                          "database": "pick-mgmt-db",
                          "namespace": "fulfillment-platform"
                        },
                        {
                          "name": "delivery-mgmt",
                          "team": "last-mile",
                          "tier": 1,
                          "slo": { "p99_latency_ms": 600, "error_rate_pct": 0.5, "availability_pct": 99.95 },
                          "database": "delivery-mgmt-db",
                          "namespace": "last-mile"
                        }
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, serviceInventory);
    }
}
