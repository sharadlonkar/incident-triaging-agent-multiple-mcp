package com.walmart.mcp.openobserve.config;

import com.walmart.mcp.openobserve.service.OpenObserveService;
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
    public ToolCallbackProvider openObserveTools(OpenObserveService openObserveService) {
        return MethodToolCallbackProvider.builder().toolObjects(openObserveService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> openObservePrompts() {
        var logInvestigationPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("investigate-service-logs",
                "Perform a comprehensive log investigation for an incident in a specific service",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to investigate", true),
                         new McpSchema.PromptArgument("minutes", "Lookback window in minutes", true))),
            req -> new McpSchema.GetPromptResult("Log investigation",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Investigate logs for '" + req.arguments().get("serviceName") +
                        "' over the last " + req.arguments().get("minutes") + " minutes. " +
                        "Start with error logs, then get exception summary, and analyze volume trends. " +
                        "Identify the primary error type, when it started, and how fast it's growing."
                    ))))
        );

        var orderTracePrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("trace-order-journey",
                "Trace a specific order through all services to find where it failed",
                List.of(new McpSchema.PromptArgument("orderId", "Order ID (e.g. WMT-98765)", true))),
            req -> new McpSchema.GetPromptResult("Order trace",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Trace order '" + req.arguments().get("orderId") +
                        "' through all services using the audit trail. " +
                        "Identify exactly which service and step it failed at, " +
                        "what error was returned, and what the customer experienced."
                    ))))
        );

        var exceptionAnalysisPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("analyze-exception-patterns",
                "Analyze exception patterns to identify root cause from stack traces",
                List.of(new McpSchema.PromptArgument("serviceName", "Service name", true),
                         new McpSchema.PromptArgument("minutes", "Time window", true))),
            req -> new McpSchema.GetPromptResult("Exception pattern analysis",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Analyze exception patterns for '" + req.arguments().get("serviceName") +
                        "' in the last " + req.arguments().get("minutes") + " minutes. " +
                        "Read the stack traces carefully, identify the root exception vs wrapper exceptions, " +
                        "and determine the single highest-priority fix to stop the error cascade."
                    ))))
        );

        var incidentTimelinePrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("build-incident-timeline",
                "Build a timeline of when the incident started and how it progressed using log volume data",
                List.of(new McpSchema.PromptArgument("serviceName", "Primary affected service", true),
                         new McpSchema.PromptArgument("minutes", "Total lookback window", true))),
            req -> new McpSchema.GetPromptResult("Incident timeline",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Build a chronological incident timeline for '" + req.arguments().get("serviceName") +
                        "' using log volume trends over " + req.arguments().get("minutes") + " minutes. " +
                        "Identify T0 (incident start), rate of degradation, and key inflection points. " +
                        "Format as a timeline with timestamps and events."
                    ))))
        );

        var keywordSearchPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("keyword-log-search",
                "Search logs across services for a specific error message or pattern",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to search", true),
                         new McpSchema.PromptArgument("keyword", "Error keyword or pattern", true),
                         new McpSchema.PromptArgument("minutes", "Time window", true))),
            req -> new McpSchema.GetPromptResult("Keyword search",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Search logs for '" + req.arguments().get("serviceName") + "' for keyword '" +
                        req.arguments().get("keyword") + "' over " + req.arguments().get("minutes") +
                        " minutes. Count occurrences, show representative samples, " +
                        "and determine if this keyword pattern correlates with the incident."
                    ))))
        );

        return List.of(logInvestigationPrompt, orderTracePrompt, exceptionAnalysisPrompt, incidentTimelinePrompt, keywordSearchPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> openObserveResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("openobserve://resources/tools-description",
                "OpenObserve Tools Description",
                "Description of all OpenObserve/Splunk log analytics MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("openobserve://resources/tools-description", "text/plain",
                    """
                    OpenObserve/Splunk Log Analytics MCP Server Tools
                    ===================================================

                    Centralized log aggregation and analysis for all Walmart backend services.
                    Ingests ~2TB of logs daily across 200+ microservices.

                    AVAILABLE TOOLS:

                    1. searchLogs(serviceName, query, minutes)
                       - Full-text search across service logs
                       - Returns sample matching entries with trace IDs
                       - Supports keyword, regex, and field-value queries
                       - Use for targeted investigation of specific errors

                    2. getErrorLogs(serviceName, minutes)
                       - All ERROR/FATAL level logs with frequency analysis
                       - Groups by exception type with first/last occurrence
                       - Percentage breakdown of error types
                       - Identifies accelerating vs stable error rates

                    3. getOrderAuditTrail(orderId)
                       - Complete lifecycle trace for a specific order
                       - Shows each service hop with success/failure status
                       - Identifies exactly where and why an order got stuck
                       - Includes trace IDs for cross-service correlation

                    4. getLogVolumeTrend(serviceName, minutes, bucketSizeMinutes)
                       - Time-bucketed log volume and error rate trends
                       - Pinpoints exact incident start time
                       - Shows error rate progression (stable, accelerating, recovering)
                       - Correlates with deployment or config change events

                    5. getExceptionSummary(serviceName, minutes)
                       - Ranked exception types by frequency with full stack traces
                       - Identifies root exception vs wrapper/cascade exceptions
                       - Maps exceptions to affected classes and methods
                       - Critical for root cause identification from code level

                    LOG LEVELS INDEXED: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
                    RETENTION: 90 days hot, 1 year cold storage
                    LATENCY: Near real-time (< 30 second ingestion lag)
                    """)
            ))
        );

        var indexSchema = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("openobserve://resources/log-schema",
                "Log Schema and Field Reference",
                "Schema definition for log fields available for search and filtering",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("openobserve://resources/log-schema", "application/json",
                    """
                    {
                      "logSchema": {
                        "timestamp": "ISO-8601 UTC timestamp",
                        "level": "TRACE|DEBUG|INFO|WARN|ERROR|FATAL",
                        "service": "microservice name",
                        "instance": "pod/container name",
                        "thread": "thread name",
                        "class": "Java fully-qualified class name",
                        "method": "method name",
                        "message": "log message text",
                        "exception": "exception class name",
                        "stackTrace": "full stack trace",
                        "traceId": "distributed trace ID (OpenTelemetry)",
                        "spanId": "span ID",
                        "orderId": "order identifier if present",
                        "customerId": "customer ID if present",
                        "userId": "internal user ID",
                        "requestId": "HTTP request ID",
                        "httpMethod": "GET|POST|PUT|DELETE",
                        "httpPath": "request URI path",
                        "httpStatus": "response HTTP status code",
                        "durationMs": "request duration in milliseconds",
                        "env": "prod|staging|dev"
                      },
                      "searchableServices": [
                        "order-mgmt", "fulfillment-mgmt", "pre-pick-mgmt",
                        "pick-mgmt", "post-pick-mgmt", "delivery-mgmt",
                        "api-gateway", "auth-service", "inventory-service",
                        "payment-service", "notification-service", "warehouse-service"
                      ]
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, indexSchema);
    }
}
