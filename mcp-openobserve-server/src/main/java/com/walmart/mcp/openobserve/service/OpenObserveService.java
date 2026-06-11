package com.walmart.mcp.openobserve.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * OpenObserve/Splunk MCP service providing log analytics and search capabilities.
 * Simulates centralized log aggregation for all Walmart backend services.
 */
@Service
public class OpenObserveService {

    @Tool(description = "Search application logs for a service using a keyword or pattern over the last X minutes. Returns matching log entries with timestamps, log level, and message. Use for finding specific errors or patterns.")
    public String searchLogs(String serviceName, String query, int minutes) {
        if (serviceName.equalsIgnoreCase("order-mgmt")) {
            return """
                {
                  "service": "order-mgmt",
                  "query": "%s",
                  "timeRange": "%d minutes",
                  "totalMatches": 52341,
                  "sampleLogs": [
                    {
                      "timestamp": "2026-06-11T16:29:48.234Z",
                      "level": "ERROR",
                      "thread": "hikari-pool-1",
                      "class": "com.zaxxer.hikari.pool.HikariPool",
                      "message": "HikariPool-1 - Connection is not available, request timed out after 30000ms",
                      "traceId": "trace-a9f2e4c1",
                      "orderId": "WMT-98765"
                    },
                    {
                      "timestamp": "2026-06-11T16:29:48.235Z",
                      "level": "ERROR",
                      "class": "com.walmart.ordermgmt.service.OrderProcessingService",
                      "message": "Failed to process order WMT-98765: Unable to acquire JDBC Connection",
                      "exception": "org.springframework.dao.DataAccessResourceFailureException",
                      "traceId": "trace-a9f2e4c1"
                    },
                    {
                      "timestamp": "2026-06-11T16:29:47.102Z",
                      "level": "WARN",
                      "class": "com.walmart.ordermgmt.service.OrderProcessingService",
                      "message": "Order processing queue depth: 847 pending orders",
                      "traceId": "trace-b1c3d5e7"
                    },
                    {
                      "timestamp": "2026-06-11T16:29:46.888Z",
                      "level": "ERROR",
                      "class": "com.walmart.ordermgmt.controller.OrderController",
                      "message": "Request timeout for POST /api/v2/orders after 30001ms — returning 503",
                      "customerId": "CUST-442198",
                      "traceId": "trace-f8a2b4c6"
                    }
                  ]
                }""".formatted(query, minutes);
        }
        return """
            {
              "service": "%s",
              "query": "%s",
              "timeRange": "%d minutes",
              "totalMatches": 12,
              "sampleLogs": [
                {
                  "timestamp": "2026-06-11T16:25:00.000Z",
                  "level": "INFO",
                  "message": "Service operating normally"
                }
              ]
            }""".formatted(serviceName, query, minutes);
    }

    @Tool(description = "Get all ERROR and FATAL level logs for a service in the last X minutes. Returns error count, unique error types, stack traces, and frequency analysis. Essential for identifying exception patterns.")
    public String getErrorLogs(String serviceName, int minutes) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "timeRange": "%d minutes",
                  "totalErrors": 52341,
                  "errorRate": "34.7%% of all log events",
                  "topErrorTypes": [
                    {
                      "exception": "org.springframework.dao.DataAccessResourceFailureException",
                      "count": 38420,
                      "percentage": 73.4,
                      "rootMessage": "Unable to acquire JDBC Connection; nested exception is org.hibernate.exception.JDBCConnectionException",
                      "firstOccurrence": "2026-06-11T14:23:04Z",
                      "lastOccurrence": "2026-06-11T16:29:58Z"
                    },
                    {
                      "exception": "java.util.concurrent.TimeoutException",
                      "count": 9820,
                      "percentage": 18.8,
                      "rootMessage": "Timeout waiting for connection from pool after 30000ms",
                      "firstOccurrence": "2026-06-11T14:23:12Z"
                    },
                    {
                      "exception": "org.springframework.web.client.ResourceAccessException",
                      "count": 2840,
                      "percentage": 5.4,
                      "rootMessage": "I/O error on POST request for fulfillment-mgmt",
                      "firstOccurrence": "2026-06-11T14:28:30Z"
                    },
                    {
                      "exception": "java.lang.OutOfMemoryError",
                      "count": 3,
                      "percentage": 0.006,
                      "rootMessage": "Java heap space — GC overhead limit exceeded",
                      "firstOccurrence": "2026-06-11T15:42:00Z"
                    }
                  ],
                  "errorTrend": "ACCELERATING — errors increasing 8%% per 10-minute window",
                  "criticalStackTrace": {
                    "exception": "DataAccessResourceFailureException",
                    "at": "com.walmart.ordermgmt.repository.OrderRepository.save(OrderRepository.java:89)",
                    "causedBy": "HikariPool timeout after 30000ms waiting for connection"
                  }
                }""".formatted(minutes);
            case "fulfillment-mgmt" -> """
                {
                  "service": "fulfillment-mgmt",
                  "timeRange": "%d minutes",
                  "totalErrors": 21040,
                  "errorRate": "22.3%% of all log events",
                  "topErrorTypes": [
                    {
                      "exception": "feign.RetryableException",
                      "count": 16240,
                      "rootMessage": "Connection refused executing POST http://order-mgmt/api/v2/orders/validate"
                    },
                    {
                      "exception": "java.net.SocketTimeoutException",
                      "count": 4800,
                      "rootMessage": "Read timed out after 3000ms calling order-mgmt"
                    }
                  ]
                }""".formatted(minutes);
            default -> """
                {
                  "service": "%s",
                  "timeRange": "%d minutes",
                  "totalErrors": 8,
                  "errorRate": "0.02%%",
                  "status": "NORMAL"
                }""".formatted(serviceName, minutes);
        };
    }

    @Tool(description = "Get audit trail logs for a specific order ID showing the complete order lifecycle across all services. Traces the order through order-mgmt, fulfillment-mgmt, pick-mgmt, and delivery-mgmt.")
    public String getOrderAuditTrail(String orderId) {
        return switch (orderId.toUpperCase()) {
            case "WMT-98765" -> """
                {
                  "orderId": "WMT-98765",
                  "customerId": "CUST-442198",
                  "orderValue": "$284.99",
                  "createdAt": "2026-06-11T14:21:30Z",
                  "currentStatus": "STUCK_IN_PROCESSING",
                  "auditTrail": [
                    {
                      "timestamp": "2026-06-11T14:21:30Z",
                      "service": "api-gateway",
                      "event": "ORDER_RECEIVED",
                      "status": "SUCCESS",
                      "details": "POST /api/v2/orders received from customer CUST-442198"
                    },
                    {
                      "timestamp": "2026-06-11T14:21:31Z",
                      "service": "order-mgmt",
                      "event": "ORDER_VALIDATION_START",
                      "status": "SUCCESS",
                      "details": "Order validation initiated, payment pre-authorized"
                    },
                    {
                      "timestamp": "2026-06-11T14:21:31Z",
                      "service": "order-mgmt",
                      "event": "DB_WRITE_ATTEMPT",
                      "status": "FAILED",
                      "details": "Failed to persist order — HikariCP pool exhausted",
                      "exception": "DataAccessResourceFailureException",
                      "retryAttempts": 3
                    },
                    {
                      "timestamp": "2026-06-11T14:22:31Z",
                      "service": "order-mgmt",
                      "event": "ORDER_PROCESSING_TIMEOUT",
                      "status": "FAILED",
                      "details": "Order processing exceeded 60s timeout — returned 503 to gateway",
                      "traceId": "trace-a9f2e4c1"
                    },
                    {
                      "timestamp": "2026-06-11T14:22:32Z",
                      "service": "api-gateway",
                      "event": "ERROR_RETURNED_TO_CLIENT",
                      "status": "FAILED",
                      "httpStatus": 503,
                      "details": "503 Service Unavailable returned to customer"
                    }
                  ],
                  "lastStuck": "order-mgmt at DB_WRITE_ATTEMPT step",
                  "recommendation": "Order may be retried safely after order-mgmt recovery — payment pre-auth is still valid for 2 hours"
                }""";
            default -> """
                {
                  "orderId": "%s",
                  "status": "NOT_FOUND",
                  "message": "No logs found for this order ID in the last 24 hours"
                }""".formatted(orderId);
        };
    }

    @Tool(description = "Get log volume and error rate trends for a service over the last X minutes, broken into time buckets. Useful for identifying when an incident started and its rate of progression.")
    public String getLogVolumeTrend(String serviceName, int minutes, int bucketSizeMinutes) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "timeRange": "%d minutes",
                  "bucketSize": "%d minutes",
                  "trend": "DETERIORATING",
                  "buckets": [
                    { "time": "13:58-14:03", "totalLogs": 4820, "errors": 12, "errorRate": "0.25%%", "note": "v2.4.1 deployed" },
                    { "time": "14:03-14:08", "totalLogs": 4910, "errors": 48, "errorRate": "0.98%%", "note": "Slight increase" },
                    { "time": "14:08-14:13", "totalLogs": 5120, "errors": 342, "errorRate": "6.68%%", "note": "Pool filling up" },
                    { "time": "14:13-14:18", "totalLogs": 5840, "errors": 1842, "errorRate": "31.5%%", "note": "Pool near saturation" },
                    { "time": "14:18-14:23", "totalLogs": 6240, "errors": 2841, "errorRate": "45.5%%", "note": "Pool exhausted" },
                    { "time": "14:23-14:28", "totalLogs": 7120, "errors": 4210, "errorRate": "59.1%%", "note": "Full failure begins" },
                    { "time": "16:20-16:25", "totalLogs": 8940, "errors": 5284, "errorRate": "59.1%%", "note": "Still at peak failure" },
                    { "time": "16:25-16:30", "totalLogs": 9120, "errors": 5401, "errorRate": "59.2%%", "note": "No recovery yet" }
                  ],
                  "incidentStartEstimate": "2026-06-11T14:23:00Z",
                  "incidentTrigger": "order-mgmt v2.4.1 deployment at 13:58 UTC reduced HikariCP pool"
                }""".formatted(minutes, bucketSizeMinutes);
            default -> """
                {
                  "service": "%s",
                  "timeRange": "%d minutes",
                  "bucketSize": "%d minutes",
                  "trend": "STABLE",
                  "buckets": []
                }""".formatted(serviceName, minutes, bucketSizeMinutes);
        };
    }

    @Tool(description = "Get exception stack traces and frequency for a service in the last X minutes. Groups exceptions by type and provides full stack trace for the most recent occurrence of each type.")
    public String getExceptionSummary(String serviceName, int minutes) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "timeRange": "%d minutes",
                  "totalExceptions": 52341,
                  "uniqueExceptionTypes": 4,
                  "topExceptions": [
                    {
                      "rank": 1,
                      "type": "DataAccessResourceFailureException",
                      "package": "org.springframework.dao",
                      "occurrences": 38420,
                      "firstSeen": "2026-06-11T14:23:04Z",
                      "affectedClasses": ["OrderRepository", "OrderItemRepository", "OrderStatusRepository"],
                      "fullStackTrace": [
                        "org.springframework.dao.DataAccessResourceFailureException: Unable to acquire JDBC Connection",
                        "  at org.springframework.orm.jpa.vendor.HibernateJpaDialect.translateExceptionIfPossible(HibernateJpaDialect.java:235)",
                        "  at com.walmart.ordermgmt.repository.OrderRepository.save(OrderRepository.java:89)",
                        "  at com.walmart.ordermgmt.service.OrderProcessingService.createOrder(OrderProcessingService.java:142)",
                        "  at com.walmart.ordermgmt.controller.OrderController.createOrder(OrderController.java:67)",
                        "Caused by: com.zaxxer.hikari.pool.HikariPool$PoolTimeoutException: HikariPool-1 - Connection is not available, request timed out after 30000ms",
                        "  at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:213)"
                      ]
                    },
                    {
                      "rank": 2,
                      "type": "TimeoutException",
                      "package": "java.util.concurrent",
                      "occurrences": 9820,
                      "affectedClasses": ["OrderProcessingService"],
                      "summary": "Timeout waiting for DB connection from HikariCP pool after 30000ms"
                    },
                    {
                      "rank": 3,
                      "type": "ResourceAccessException",
                      "package": "org.springframework.web.client",
                      "occurrences": 2840,
                      "summary": "Downstream fulfillment-mgmt calls failing due to order-mgmt instability"
                    },
                    {
                      "rank": 4,
                      "type": "OutOfMemoryError",
                      "package": "java.lang",
                      "occurrences": 3,
                      "severity": "CRITICAL",
                      "summary": "JVM heap exhausted — GC overhead limit exceeded on pods"
                    }
                  ]
                }""".formatted(minutes);
            default -> """
                {
                  "service": "%s",
                  "timeRange": "%d minutes",
                  "totalExceptions": 5,
                  "uniqueExceptionTypes": 2,
                  "status": "NORMAL"
                }""".formatted(serviceName, minutes);
        };
    }
}
