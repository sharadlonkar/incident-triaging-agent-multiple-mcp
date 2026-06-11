package com.walmart.mcp.mms.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * MMS (Metric Management System) - Prometheus/Grafana integration.
 * Provides metrics for App, Database, and Infrastructure layers.
 *
 * Hardcoded scenario: order-mgmt service degradation due to DB pool exhaustion
 * after deployment of v2.4.1. Incident started ~2 hours ago.
 */
@Service
public class MmsService {

    @Tool(description = "Get latency percentile metrics (p50, p90, p99) for a microservice over the last X minutes from Prometheus. Service names: order-mgmt, fulfillment-mgmt, pre-pick-mgmt, pick-mgmt, post-pick-mgmt, delivery-mgmt")
    public String getServiceLatency(String serviceName, int minutes) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "timeRange": "%d minutes",
                  "status": "DEGRADED",
                  "metrics": {
                    "p50_ms": 2340,
                    "p90_ms": 8920,
                    "p99_ms": 15400,
                    "baseline_p99_ms": 320,
                    "requestRate": "1842/min",
                    "errorRate": "34.7%%",
                    "trend": "INCREASING"
                  },
                  "annotation": "Latency spike started at 14:23 UTC, correlates with order-mgmt v2.4.1 deployment at 13:58 UTC"
                }""".formatted(minutes);
            case "fulfillment-mgmt" -> """
                {
                  "service": "fulfillment-mgmt",
                  "timeRange": "%d minutes",
                  "status": "DEGRADED",
                  "metrics": {
                    "p50_ms": 1820,
                    "p90_ms": 5400,
                    "p99_ms": 9800,
                    "baseline_p99_ms": 280,
                    "requestRate": "943/min",
                    "errorRate": "22.3%%",
                    "trend": "INCREASING"
                  },
                  "annotation": "Degradation cascading from order-mgmt timeouts"
                }""".formatted(minutes);
            case "pre-pick-mgmt" -> """
                {
                  "service": "pre-pick-mgmt",
                  "timeRange": "%d minutes",
                  "status": "HEALTHY",
                  "metrics": {
                    "p50_ms": 145,
                    "p90_ms": 290,
                    "p99_ms": 420,
                    "baseline_p99_ms": 380,
                    "requestRate": "421/min",
                    "errorRate": "0.2%%",
                    "trend": "STABLE"
                  }
                }""".formatted(minutes);
            default -> """
                {
                  "service": "%s",
                  "timeRange": "%d minutes",
                  "status": "HEALTHY",
                  "metrics": {
                    "p50_ms": 120,
                    "p90_ms": 240,
                    "p99_ms": 380,
                    "requestRate": "350/min",
                    "errorRate": "0.1%%",
                    "trend": "STABLE"
                  }
                }""".formatted(serviceName, minutes);
        };
    }

    @Tool(description = "Get error rate metrics and HTTP status code breakdown for a service in the last X minutes. Returns 4xx and 5xx error counts and percentages.")
    public String getErrorRate(String serviceName, int minutes) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "timeRange": "%d minutes",
                  "totalRequests": 184200,
                  "errorBreakdown": {
                    "5xx": { "count": 52340, "percentage": 28.4, "dominant": "503 Service Unavailable" },
                    "4xx": { "count": 11800, "percentage": 6.4, "dominant": "429 Too Many Requests" },
                    "timeout": { "count": 9820, "percentage": 5.3 }
                  },
                  "overallErrorRate": "34.7%%",
                  "affectedEndpoints": [
                    { "path": "/api/v2/orders", "errorRate": "41.2%%", "method": "POST" },
                    { "path": "/api/v2/orders/{id}/status", "errorRate": "38.9%%", "method": "GET" },
                    { "path": "/api/v2/orders/batch", "errorRate": "29.1%%", "method": "POST" }
                  ],
                  "rootCauseIndicator": "DB connection pool exhausted - HikariCP timeout errors"
                }""".formatted(minutes);
            case "fulfillment-mgmt" -> """
                {
                  "service": "fulfillment-mgmt",
                  "timeRange": "%d minutes",
                  "totalRequests": 94300,
                  "errorBreakdown": {
                    "5xx": { "count": 16240, "percentage": 17.2, "dominant": "502 Bad Gateway" },
                    "4xx": { "count": 4800, "percentage": 5.1, "dominant": "408 Request Timeout" },
                    "timeout": { "count": 3200, "percentage": 3.4 }
                  },
                  "overallErrorRate": "22.3%%",
                  "affectedEndpoints": [
                    { "path": "/api/v1/fulfillment/assign", "errorRate": "31.2%%", "method": "POST" },
                    { "path": "/api/v1/fulfillment/status", "errorRate": "18.4%%", "method": "GET" }
                  ],
                  "rootCauseIndicator": "Upstream order-mgmt timeouts causing cascade failures"
                }""".formatted(minutes);
            default -> """
                {
                  "service": "%s",
                  "timeRange": "%d minutes",
                  "totalRequests": 42000,
                  "overallErrorRate": "0.15%%",
                  "errorBreakdown": {
                    "5xx": { "count": 42, "percentage": 0.1 },
                    "4xx": { "count": 21, "percentage": 0.05 }
                  }
                }""".formatted(serviceName, minutes);
        };
    }

    @Tool(description = "Get database metrics including connection pool usage, query latency, slow queries, and deadlocks for a given database instance in the last X minutes")
    public String getDatabaseMetrics(String dbInstance, int minutes) {
        return switch (dbInstance.toLowerCase()) {
            case "order-mgmt-db", "order_mgmt_db" -> """
                {
                  "dbInstance": "order-mgmt-db",
                  "type": "PostgreSQL 15.2",
                  "timeRange": "%d minutes",
                  "status": "CRITICAL",
                  "connectionPool": {
                    "maxPoolSize": 100,
                    "activeConnections": 99,
                    "idleConnections": 1,
                    "pendingAcquisitions": 847,
                    "acquisitionTimeoutErrors": 15234,
                    "poolUtilization": "99%%"
                  },
                  "queryMetrics": {
                    "avgQueryTime_ms": 4320,
                    "slowQueries_count": 1842,
                    "slowQueryThreshold_ms": 1000,
                    "deadlocks": 23,
                    "longRunningQueries": [
                      { "query": "SELECT * FROM orders WHERE status = 'PENDING' ORDER BY created_at", "duration_ms": 34200, "locks": 847 },
                      { "query": "UPDATE order_items SET status = 'PROCESSING'", "duration_ms": 28100, "locks": 412 }
                    ]
                  },
                  "replication": { "lag_ms": 4800, "status": "DEGRADED" },
                  "alert": "HikariCP pool exhaustion - JDBC connection timeouts since 14:23 UTC"
                }""".formatted(minutes);
            case "fulfillment-mgmt-db", "fulfillment_mgmt_db" -> """
                {
                  "dbInstance": "fulfillment-mgmt-db",
                  "type": "PostgreSQL 15.2",
                  "timeRange": "%d minutes",
                  "status": "WARNING",
                  "connectionPool": {
                    "maxPoolSize": 80,
                    "activeConnections": 71,
                    "idleConnections": 9,
                    "poolUtilization": "88.75%%"
                  },
                  "queryMetrics": {
                    "avgQueryTime_ms": 1240,
                    "slowQueries_count": 234,
                    "deadlocks": 5
                  }
                }""".formatted(minutes);
            default -> """
                {
                  "dbInstance": "%s",
                  "timeRange": "%d minutes",
                  "status": "HEALTHY",
                  "connectionPool": { "poolUtilization": "32%%", "activeConnections": 26 },
                  "queryMetrics": { "avgQueryTime_ms": 45, "slowQueries_count": 2 }
                }""".formatted(dbInstance, minutes);
        };
    }

    @Tool(description = "Get infrastructure metrics (CPU, memory, disk, network I/O) for a specific host or node over the last X minutes from Grafana/Prometheus node exporter")
    public String getInfraMetrics(String hostName, int minutes) {
        return switch (hostName.toLowerCase()) {
            case "wmt-prod-node-07", "node-07" -> """
                {
                  "host": "wmt-prod-node-07",
                  "cluster": "walmart-prod-us-central",
                  "timeRange": "%d minutes",
                  "status": "WARNING",
                  "cpu": { "usage_pct": 94.2, "iowait_pct": 18.4, "trend": "HIGH" },
                  "memory": { "used_gb": 61.8, "total_gb": 64, "used_pct": 96.6, "swapUsed_gb": 3.2 },
                  "disk": { "readOps_per_sec": 8420, "writeOps_per_sec": 12840, "latency_ms": 28 },
                  "network": { "rxMbps": 842, "txMbps": 1240 },
                  "processes": { "zombieCount": 3, "oomKillerEvents": 2 },
                  "note": "Hosts order-mgmt pods, high memory pressure causing GC storms"
                }""".formatted(minutes);
            case "wmt-prod-node-08", "node-08" -> """
                {
                  "host": "wmt-prod-node-08",
                  "cluster": "walmart-prod-us-central",
                  "timeRange": "%d minutes",
                  "status": "WARNING",
                  "cpu": { "usage_pct": 78.4, "iowait_pct": 12.1 },
                  "memory": { "used_pct": 82.3, "used_gb": 52.7, "total_gb": 64 },
                  "disk": { "readOps_per_sec": 4200, "writeOps_per_sec": 6100 },
                  "note": "Hosts fulfillment-mgmt pods"
                }""".formatted(minutes);
            default -> """
                {
                  "host": "%s",
                  "timeRange": "%d minutes",
                  "status": "HEALTHY",
                  "cpu": { "usage_pct": 34.2 },
                  "memory": { "used_pct": 52.1 },
                  "disk": { "latency_ms": 4 }
                }""".formatted(hostName, minutes);
        };
    }

    @Tool(description = "Get all active Prometheus/Grafana alerts filtered by severity (CRITICAL, WARNING, INFO) and optionally by service name. Returns firing alerts with details.")
    public String getActiveAlerts(String severity, String serviceName) {
        String svcFilter = serviceName != null && !serviceName.isBlank() ? serviceName : "all";
        return """
            {
              "queryTime": "2026-06-11T16:30:00Z",
              "severity": "%s",
              "serviceFilter": "%s",
              "totalAlerts": 7,
              "alerts": [
                {
                  "id": "ALT-4421",
                  "name": "HighErrorRate",
                  "severity": "CRITICAL",
                  "service": "order-mgmt",
                  "message": "Error rate 34.7%% exceeds critical threshold of 5%%",
                  "firingFor": "2h 7m",
                  "labels": { "env": "prod", "cluster": "walmart-prod-us-central", "team": "order-platform" }
                },
                {
                  "id": "ALT-4422",
                  "name": "DBConnectionPoolExhausted",
                  "severity": "CRITICAL",
                  "service": "order-mgmt",
                  "message": "HikariCP pool 99/100 active, 847 pending acquisitions",
                  "firingFor": "2h 5m",
                  "labels": { "db": "order-mgmt-db", "env": "prod" }
                },
                {
                  "id": "ALT-4423",
                  "name": "HighLatency",
                  "severity": "CRITICAL",
                  "service": "order-mgmt",
                  "message": "p99 latency 15400ms exceeds SLO threshold of 500ms",
                  "firingFor": "2h 3m"
                },
                {
                  "id": "ALT-4424",
                  "name": "CascadeFailure",
                  "severity": "WARNING",
                  "service": "fulfillment-mgmt",
                  "message": "Error rate 22.3%%, cascade from order-mgmt",
                  "firingFor": "1h 52m"
                },
                {
                  "id": "ALT-4425",
                  "name": "PodCrashLooping",
                  "severity": "CRITICAL",
                  "service": "order-mgmt",
                  "message": "3 pods in CrashLoopBackOff in namespace order-platform",
                  "firingFor": "1h 45m"
                },
                {
                  "id": "ALT-4426",
                  "name": "NodeMemoryPressure",
                  "severity": "WARNING",
                  "service": "infrastructure",
                  "message": "wmt-prod-node-07 memory usage 96.6%%, OOM risk",
                  "firingFor": "1h 30m"
                },
                {
                  "id": "ALT-4427",
                  "name": "ReplicationLag",
                  "severity": "WARNING",
                  "service": "order-mgmt",
                  "message": "DB replication lag 4800ms, RPO risk",
                  "firingFor": "1h 58m"
                }
              ]
            }""".formatted(severity, svcFilter);
    }
}
