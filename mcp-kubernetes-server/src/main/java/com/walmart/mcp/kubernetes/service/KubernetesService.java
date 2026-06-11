package com.walmart.mcp.kubernetes.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Kubernetes MCP service providing cluster and workload health information.
 * Simulates kubectl and Kubernetes API server access for the Walmart prod cluster.
 */
@Service
public class KubernetesService {

    @Tool(description = "Get the status of all pods for a service in a Kubernetes namespace. Returns pod names, status (Running/CrashLoopBackOff/Pending/OOMKilled), restart counts, age, and resource consumption.")
    public String getPodStatus(String namespace, String serviceName) {
        String key = namespace.toLowerCase() + "/" + serviceName.toLowerCase();
        return switch (key) {
            case "order-platform/order-mgmt" -> """
                {
                  "namespace": "order-platform",
                  "service": "order-mgmt",
                  "deployment": "order-mgmt-v2.4.1",
                  "desiredReplicas": 8,
                  "readyReplicas": 5,
                  "availableReplicas": 5,
                  "pods": [
                    { "name": "order-mgmt-7d9f8b-xk2pq", "status": "Running", "restarts": 0, "age": "25m", "node": "wmt-prod-node-07", "cpu": "1840m/2000m", "memory": "3.8Gi/4Gi", "ready": true },
                    { "name": "order-mgmt-7d9f8b-mn4rs", "status": "Running", "restarts": 2, "age": "25m", "node": "wmt-prod-node-07", "cpu": "1920m/2000m", "memory": "3.9Gi/4Gi", "ready": true },
                    { "name": "order-mgmt-7d9f8b-pq7tv", "status": "CrashLoopBackOff", "restarts": 8, "age": "25m", "node": "wmt-prod-node-07", "lastExitReason": "OOMKilled", "ready": false },
                    { "name": "order-mgmt-7d9f8b-uv1wx", "status": "CrashLoopBackOff", "restarts": 6, "age": "25m", "node": "wmt-prod-node-08", "lastExitReason": "OOMKilled", "ready": false },
                    { "name": "order-mgmt-7d9f8b-yz3ab", "status": "CrashLoopBackOff", "restarts": 5, "age": "25m", "node": "wmt-prod-node-08", "lastExitReason": "OOMKilled", "ready": false },
                    { "name": "order-mgmt-7d9f8b-cd5ef", "status": "Running", "restarts": 1, "age": "25m", "node": "wmt-prod-node-09", "cpu": "1760m/2000m", "memory": "3.7Gi/4Gi", "ready": true },
                    { "name": "order-mgmt-7d9f8b-gh7ij", "status": "Running", "restarts": 1, "age": "25m", "node": "wmt-prod-node-09", "cpu": "1800m/2000m", "memory": "3.8Gi/4Gi", "ready": true },
                    { "name": "order-mgmt-7d9f8b-kl9mn", "status": "Running", "restarts": 0, "age": "25m", "node": "wmt-prod-node-10", "cpu": "1820m/2000m", "memory": "3.7Gi/4Gi", "ready": true }
                  ],
                  "alerts": [
                    "3/8 pods in CrashLoopBackOff (OOMKilled) — JVM heap limit 4Gi insufficient",
                    "All running pods at >90%% CPU and >92%% memory — high risk of further OOMKills",
                    "Effective capacity reduced to 62.5%% (5/8 pods) — insufficient for peak traffic"
                  ]
                }""";
            case "fulfillment-platform/fulfillment-mgmt" -> """
                {
                  "namespace": "fulfillment-platform",
                  "service": "fulfillment-mgmt",
                  "desiredReplicas": 6,
                  "readyReplicas": 6,
                  "pods": [
                    { "name": "fulfillment-mgmt-5c8d-abc12", "status": "Running", "restarts": 0, "cpu": "1240m/1500m", "memory": "2.8Gi/3Gi", "ready": true },
                    { "name": "fulfillment-mgmt-5c8d-def34", "status": "Running", "restarts": 1, "cpu": "1380m/1500m", "memory": "2.9Gi/3Gi", "ready": true },
                    { "name": "fulfillment-mgmt-5c8d-ghi56", "status": "Running", "restarts": 0, "cpu": "1210m/1500m", "memory": "2.7Gi/3Gi", "ready": true },
                    { "name": "fulfillment-mgmt-5c8d-jkl78", "status": "Running", "restarts": 0, "cpu": "1290m/1500m", "memory": "2.8Gi/3Gi", "ready": true },
                    { "name": "fulfillment-mgmt-5c8d-mno90", "status": "Running", "restarts": 1, "cpu": "1350m/1500m", "memory": "2.9Gi/3Gi", "ready": true },
                    { "name": "fulfillment-mgmt-5c8d-pqr12", "status": "Running", "restarts": 0, "cpu": "1180m/1500m", "memory": "2.7Gi/3Gi", "ready": true }
                  ],
                  "alerts": ["All pods running but under high load due to upstream order-mgmt retries"]
                }""";
            default -> """
                {
                  "namespace": "%s",
                  "service": "%s",
                  "status": "HEALTHY",
                  "readyReplicas": 4,
                  "desiredReplicas": 4,
                  "alerts": []
                }""".formatted(namespace, serviceName);
        };
    }

    @Tool(description = "Get Kubernetes node health and resource utilization for all nodes in the production cluster. Returns CPU, memory, pod count, and any node conditions like MemoryPressure or DiskPressure.")
    public String getNodeHealth() {
        return """
            {
              "cluster": "walmart-prod-us-central",
              "totalNodes": 24,
              "healthyNodes": 22,
              "problematicNodes": 2,
              "nodes": [
                {
                  "name": "wmt-prod-node-07",
                  "status": "Ready",
                  "conditions": ["MemoryPressure"],
                  "cpu": { "allocatable": "32 cores", "requested": "30.4 cores (95%%)", "limit": "32 cores" },
                  "memory": { "allocatable": "64Gi", "requested": "61.8Gi (96.6%%)", "limit": "64Gi" },
                  "pods": { "running": 28, "capacity": 30, "crashLoopCount": 2 },
                  "labels": { "workload": "order-platform", "zone": "us-central-a" }
                },
                {
                  "name": "wmt-prod-node-08",
                  "status": "Ready",
                  "conditions": ["MemoryPressure"],
                  "cpu": { "allocatable": "32 cores", "requested": "25.6 cores (80%%)" },
                  "memory": { "allocatable": "64Gi", "requested": "52.7Gi (82.3%%)" },
                  "pods": { "running": 26, "capacity": 30, "crashLoopCount": 1 },
                  "labels": { "workload": "fulfillment-platform", "zone": "us-central-b" }
                },
                {
                  "name": "wmt-prod-node-09",
                  "status": "Ready",
                  "conditions": [],
                  "cpu": { "allocatable": "32 cores", "requested": "18.4 cores (57.5%%)" },
                  "memory": { "allocatable": "64Gi", "requested": "34.2Gi (53.4%%)" },
                  "pods": { "running": 18, "capacity": 30 }
                },
                {
                  "name": "wmt-prod-node-10",
                  "status": "Ready",
                  "conditions": [],
                  "cpu": { "allocatable": "32 cores", "requested": "16.8 cores (52.5%%)" },
                  "memory": { "allocatable": "64Gi", "requested": "28.4Gi (44.4%%)" },
                  "pods": { "running": 16, "capacity": 30 }
                }
              ],
              "clusterSummary": "2 nodes under MemoryPressure hosting order-mgmt and fulfillment-mgmt workloads"
            }""";
    }

    @Tool(description = "Get Kubernetes deployment status and rollout history for a service. Shows current rollout status, strategy, and last 5 deployment revisions with their timestamps.")
    public String getDeploymentStatus(String namespace, String deploymentName) {
        String key = namespace.toLowerCase() + "/" + deploymentName.toLowerCase();
        return switch (key) {
            case "order-platform/order-mgmt" -> """
                {
                  "namespace": "order-platform",
                  "deployment": "order-mgmt",
                  "currentRevision": "order-mgmt-v2.4.1",
                  "rolloutStatus": "FAILED",
                  "strategy": "RollingUpdate (maxUnavailable: 25%%, maxSurge: 25%%)",
                  "replicas": { "desired": 8, "updated": 8, "ready": 5, "available": 5 },
                  "conditions": [
                    { "type": "Available", "status": "False", "reason": "MinimumReplicasUnavailable" },
                    { "type": "Progressing", "status": "True", "reason": "ReplicaSetUpdated" }
                  ],
                  "rolloutHistory": [
                    {
                      "revision": 12,
                      "version": "v2.4.1",
                      "image": "walmart-registry.io/order-mgmt:2.4.1",
                      "deployedAt": "2026-06-11T13:58:00Z",
                      "deployedBy": "infiniti-pipeline-sa",
                      "status": "DEGRADED",
                      "changeLog": "Feature: batch order processing. Config change: reduced DB pool from 200 to 100"
                    },
                    {
                      "revision": 11,
                      "version": "v2.4.0",
                      "image": "walmart-registry.io/order-mgmt:2.4.0",
                      "deployedAt": "2026-06-09T10:15:00Z",
                      "status": "SUPERSEDED",
                      "available": true
                    },
                    {
                      "revision": 10,
                      "version": "v2.3.9",
                      "image": "walmart-registry.io/order-mgmt:2.3.9",
                      "deployedAt": "2026-06-05T14:30:00Z",
                      "status": "SUPERSEDED"
                    }
                  ],
                  "rollbackCommand": "kubectl rollout undo deployment/order-mgmt -n order-platform --to-revision=11",
                  "estimatedRollbackTime": "3-5 minutes"
                }""";
            default -> """
                {
                  "namespace": "%s",
                  "deployment": "%s",
                  "rolloutStatus": "COMPLETE",
                  "replicas": { "desired": 4, "ready": 4, "available": 4 }
                }""".formatted(namespace, deploymentName);
        };
    }

    @Tool(description = "Get recent Kubernetes events for a namespace showing warnings, errors, and notable cluster events in the last X minutes. Useful for spotting OOMKills, scheduling issues, and probe failures.")
    public String getClusterEvents(String namespace, int minutes) {
        return switch (namespace.toLowerCase()) {
            case "order-platform" -> """
                {
                  "namespace": "order-platform",
                  "timeRange": "%d minutes",
                  "totalEvents": 284,
                  "warningEvents": 142,
                  "criticalEvents": 38,
                  "events": [
                    {
                      "timestamp": "2026-06-11T16:29:50Z",
                      "type": "Warning",
                      "reason": "OOMKilled",
                      "object": "Pod/order-mgmt-7d9f8b-pq7tv",
                      "message": "Container order-mgmt exceeded memory limit 4Gi and was OOMKilled. JVM heap dump not available.",
                      "count": 8
                    },
                    {
                      "timestamp": "2026-06-11T16:29:45Z",
                      "type": "Warning",
                      "reason": "BackOff",
                      "object": "Pod/order-mgmt-7d9f8b-uv1wx",
                      "message": "Back-off restarting failed container order-mgmt in pod order-mgmt-7d9f8b-uv1wx",
                      "count": 6
                    },
                    {
                      "timestamp": "2026-06-11T16:28:00Z",
                      "type": "Warning",
                      "reason": "FailedScheduling",
                      "object": "Pod/order-mgmt-7d9f8b-newpod",
                      "message": "0/24 nodes available: 2 Insufficient memory, 22 node(s) didn't match Pod's node affinity"
                    },
                    {
                      "timestamp": "2026-06-11T16:25:00Z",
                      "type": "Warning",
                      "reason": "Unhealthy",
                      "object": "Pod/order-mgmt-7d9f8b-mn4rs",
                      "message": "Readiness probe failed: HTTP probe failed with statuscode: 503",
                      "count": 142
                    },
                    {
                      "timestamp": "2026-06-11T14:23:00Z",
                      "type": "Warning",
                      "reason": "ScalingReplicaSet",
                      "object": "Deployment/order-mgmt",
                      "message": "Scaled down replica set order-mgmt-v2.4.0 to 0 (deployment complete) — 2h 6m ago"
                    }
                  ]
                }""".formatted(minutes);
            default -> """
                {
                  "namespace": "%s",
                  "timeRange": "%d minutes",
                  "totalEvents": 12,
                  "warningEvents": 2,
                  "events": [
                    { "type": "Normal", "reason": "Scheduled", "message": "Successfully assigned pod" }
                  ]
                }""".formatted(namespace, minutes);
        };
    }

    @Tool(description = "Get HorizontalPodAutoscaler (HPA) status and resource quota usage for a service namespace. Shows if autoscaling has hit limits and current resource quota consumption.")
    public String getScalingStatus(String namespace) {
        return switch (namespace.toLowerCase()) {
            case "order-platform" -> """
                {
                  "namespace": "order-platform",
                  "hpa": {
                    "name": "order-mgmt-hpa",
                    "minReplicas": 4,
                    "maxReplicas": 8,
                    "currentReplicas": 8,
                    "desiredReplicas": 12,
                    "status": "AT_MAX_REPLICAS — cannot scale further",
                    "scaleUpLimited": true,
                    "metrics": [
                      { "type": "cpu", "currentValue": "92%%", "targetValue": "70%%" },
                      { "type": "memory", "currentValue": "94%%", "targetValue": "80%%" }
                    ],
                    "lastScaleTime": "2026-06-11T14:35:00Z",
                    "recommendation": "Increase maxReplicas to 16 and resolve underlying DB pool issue"
                  },
                  "resourceQuota": {
                    "cpu": { "used": "30.4/32 cores", "utilization": "95%%" },
                    "memory": { "used": "61.8Gi/64Gi", "utilization": "96.6%%" },
                    "pods": { "used": "28/30", "utilization": "93.3%%" }
                  },
                  "recommendation": "HPA at max capacity. Auto-scaling cannot help. Root cause must be resolved."
                }""";
            default -> """
                {
                  "namespace": "%s",
                  "hpa": {
                    "currentReplicas": 4,
                    "desiredReplicas": 4,
                    "status": "BALANCED",
                    "scaleUpLimited": false
                  }
                }""".formatted(namespace);
        };
    }
}
