package com.walmart.mcp.kubernetes.config;

import com.walmart.mcp.kubernetes.service.KubernetesService;
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
    public ToolCallbackProvider kubernetesTools(KubernetesService kubernetesService) {
        return MethodToolCallbackProvider.builder().toolObjects(kubernetesService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> kubernetesPrompts() {
        var podHealthPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("kubernetes-pod-health-check",
                "Check the health of all pods for a service and identify crash-looping or resource-starved pods",
                List.of(new McpSchema.PromptArgument("namespace", "Kubernetes namespace", true),
                         new McpSchema.PromptArgument("serviceName", "Service/deployment name", true))),
            req -> new McpSchema.GetPromptResult("Pod health check",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check pod health for '" + req.arguments().get("serviceName") +
                        "' in namespace '" + req.arguments().get("namespace") + "'. " +
                        "Identify any pods in CrashLoopBackOff, OOMKilled, or Pending state. " +
                        "Calculate effective capacity (ready/desired ratio) and assess impact on service availability."
                    ))))
        );

        var rollbackAssessmentPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("kubernetes-rollback-assessment",
                "Assess whether a Kubernetes rollback is safe and provide rollback command",
                List.of(new McpSchema.PromptArgument("namespace", "Kubernetes namespace", true),
                         new McpSchema.PromptArgument("deploymentName", "Deployment to roll back", true))),
            req -> new McpSchema.GetPromptResult("Rollback assessment",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Assess the rollback options for deployment '" + req.arguments().get("deploymentName") +
                        "' in namespace '" + req.arguments().get("namespace") + "'. " +
                        "Check rollout history, identify the last stable revision, " +
                        "provide the exact kubectl rollback command, and estimate recovery time."
                    ))))
        );

        var clusterCapacityPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("kubernetes-cluster-capacity",
                "Assess overall cluster capacity and identify if resource exhaustion is contributing to the incident",
                List.of()),
            req -> new McpSchema.GetPromptResult("Cluster capacity",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Check cluster node health and identify nodes under memory or CPU pressure. " +
                        "Assess if resource exhaustion is preventing pod scheduling or causing OOMKills. " +
                        "Recommend capacity actions (node scaling, pod eviction, resource limit adjustment)."
                    ))))
        );

        var scalingAnalysisPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("kubernetes-scaling-analysis",
                "Analyze HPA scaling status to determine if auto-scaling can help with the current incident",
                List.of(new McpSchema.PromptArgument("namespace", "Namespace to check", true))),
            req -> new McpSchema.GetPromptResult("Scaling analysis",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Analyze HPA status for namespace '" + req.arguments().get("namespace") +
                        "'. Determine if auto-scaling has reached its maximum, " +
                        "whether scaling up would help the current incident, " +
                        "or if the problem requires a code/config fix rather than more replicas."
                    ))))
        );

        var eventInvestigationPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("kubernetes-event-investigation",
                "Investigate Kubernetes cluster events to find infrastructure-level causes of service degradation",
                List.of(new McpSchema.PromptArgument("namespace", "Namespace to check", true),
                         new McpSchema.PromptArgument("minutes", "Lookback window", true))),
            req -> new McpSchema.GetPromptResult("Event investigation",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Investigate Kubernetes events in namespace '" + req.arguments().get("namespace") +
                        "' over the last " + req.arguments().get("minutes") + " minutes. " +
                        "Focus on Warning events: OOMKilled, BackOff, FailedScheduling, Unhealthy probes. " +
                        "Build a chronological picture of infrastructure instability."
                    ))))
        );

        return List.of(podHealthPrompt, rollbackAssessmentPrompt, clusterCapacityPrompt, scalingAnalysisPrompt, eventInvestigationPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> kubernetesResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("kubernetes://resources/tools-description",
                "Kubernetes Tools Description",
                "Description of all Kubernetes MCP tools for infrastructure investigation",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("kubernetes://resources/tools-description", "text/plain",
                    """
                    Kubernetes Infrastructure MCP Server Tools
                    ===========================================

                    Provides read-only access to Walmart's production Kubernetes cluster
                    (walmart-prod-us-central) for incident investigation.

                    AVAILABLE TOOLS:

                    1. getPodStatus(namespace, serviceName)
                       - Lists all pods with status: Running/CrashLoopBackOff/Pending/OOMKilled
                       - Shows restart counts, CPU/memory consumption vs limits
                       - Calculates effective capacity (ready/desired ratio)
                       - Critical for assessing service availability impact

                    2. getNodeHealth()
                       - All 24 cluster nodes with CPU/memory utilization
                       - Node conditions: MemoryPressure, DiskPressure, NotReady
                       - Pod counts per node and workload mapping
                       - Identifies resource saturation at infrastructure level

                    3. getDeploymentStatus(namespace, deploymentName)
                       - Current rollout status and deployment health
                       - Last 5 deployment revisions with timestamps and changes
                       - Rollback command with target revision
                       - Key for change-caused incident investigation

                    4. getClusterEvents(namespace, minutes)
                       - All Warning/Normal events in a namespace
                       - OOMKill events, scheduling failures, probe failures
                       - Event count and first/last occurrence
                       - Timeline view of infrastructure instability

                    5. getScalingStatus(namespace)
                       - HPA min/max/current/desired replicas
                       - Whether auto-scaling is blocked at max
                       - Resource quota utilization
                       - Guides decision: scale out vs fix root cause

                    CLUSTER: walmart-prod-us-central
                    NAMESPACES: order-platform, fulfillment-platform, last-mile, infrastructure
                    """)
            ))
        );

        var namespaceMap = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("kubernetes://resources/namespace-service-map",
                "Namespace to Service Mapping",
                "Maps Walmart services to their Kubernetes namespaces and deployment names",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("kubernetes://resources/namespace-service-map", "application/json",
                    """
                    {
                      "cluster": "walmart-prod-us-central",
                      "namespaceServiceMap": {
                        "order-platform": {
                          "services": ["order-mgmt", "payment-service", "notification-service"],
                          "nodes": ["wmt-prod-node-07", "wmt-prod-node-08"],
                          "ingress": "order-platform-ingress"
                        },
                        "fulfillment-platform": {
                          "services": ["fulfillment-mgmt", "pre-pick-mgmt", "pick-mgmt", "post-pick-mgmt", "warehouse-service"],
                          "nodes": ["wmt-prod-node-08", "wmt-prod-node-09"],
                          "ingress": "fulfillment-ingress"
                        },
                        "last-mile": {
                          "services": ["delivery-mgmt", "route-optimizer", "driver-service"],
                          "nodes": ["wmt-prod-node-10", "wmt-prod-node-11"],
                          "ingress": "delivery-ingress"
                        },
                        "infrastructure": {
                          "services": ["api-gateway", "auth-service", "inventory-service"],
                          "nodes": ["wmt-prod-node-01", "wmt-prod-node-02"],
                          "ingress": "main-ingress"
                        }
                      }
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, namespaceMap);
    }
}
