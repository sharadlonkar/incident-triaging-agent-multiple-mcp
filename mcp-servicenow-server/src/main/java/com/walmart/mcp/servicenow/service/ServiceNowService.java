package com.walmart.mcp.servicenow.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * ServiceNow MCP service providing ITSM data including incidents, changes, and problems.
 */
@Service
public class ServiceNowService {

    @Tool(description = "Get active ServiceNow incidents for a service filtered by status. Status options: NEW, IN_PROGRESS, RESOLVED, ALL. Returns incident number, severity, description, and assignee.")
    public String getIncidents(String serviceName, String status) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "statusFilter": "%s",
                  "totalIncidents": 2,
                  "incidents": [
                    {
                      "number": "INC0042891",
                      "severity": "1 - Critical",
                      "priority": "P1",
                      "state": "In Progress",
                      "title": "order-mgmt service degradation - 34.7%% error rate, DB connection pool exhausted",
                      "description": "Production incident: order-mgmt is experiencing severe degradation with 34.7%% error rate. Root cause identified as HikariCP connection pool exhaustion (maxPoolSize reduced from 200 to 100 in v2.4.1 deployment at 13:58 UTC). Approximately 15,000+ customer orders affected. Cascade impact to fulfillment-mgmt. Revenue impact estimated $2.3M. Rollback in progress.",
                      "affectedCi": "order-mgmt (prod)",
                      "openedAt": "2026-06-11T14:30:00Z",
                      "resolvedAt": null,
                      "assignedTo": "sarah.chen",
                      "assignmentGroup": "Order Platform SRE",
                      "contactType": "Monitoring Alert (PagerDuty)",
                      "businessImpact": "HIGH — Customer-facing order placement failure",
                      "workNotes": [
                        { "time": "16:25", "by": "mike.rodriguez", "note": "Root cause confirmed: maxPoolSize config change in v2.4.1. Rollback initiated via Infiniti pipeline #4532." },
                        { "time": "16:20", "by": "priya.patel", "note": "DB pool at 99/100 connections with 847 pending acquisitions. Pool exhaustion confirmed." },
                        { "time": "14:35", "by": "sarah.chen", "note": "Incident acknowledged. Investigating metrics. Fulfillment-mgmt also impacted - cascade failure suspected." }
                      ]
                    },
                    {
                      "number": "INC0042892",
                      "severity": "2 - High",
                      "priority": "P2",
                      "state": "In Progress",
                      "title": "fulfillment-mgmt cascade degradation from order-mgmt",
                      "description": "fulfillment-mgmt experiencing 22.3%% error rate due to cascade from order-mgmt outage.",
                      "assignedTo": "james.park",
                      "openedAt": "2026-06-11T14:35:00Z"
                    }
                  ]
                }""".formatted(status);
            default -> """
                {
                  "service": "%s",
                  "statusFilter": "%s",
                  "totalIncidents": 0,
                  "incidents": []
                }""".formatted(serviceName, status);
        };
    }

    @Tool(description = "Get detailed ServiceNow incident information by incident number (e.g. INC0042891). Returns full description, work notes, timeline, and SLA status.")
    public String getIncidentDetails(String incidentNumber) {
        return switch (incidentNumber.toUpperCase()) {
            case "INC0042891" -> """
                {
                  "number": "INC0042891",
                  "severity": "1 - Critical",
                  "title": "order-mgmt service degradation",
                  "state": "In Progress",
                  "openedAt": "2026-06-11T14:30:00Z",
                  "sla": {
                    "responseTimeSLA": "15 minutes",
                    "responseStatus": "MET (responded at 14:35)",
                    "resolutionTimeSLA": "4 hours",
                    "resolutionDeadline": "2026-06-11T18:30:00Z",
                    "resolutionStatus": "AT_RISK — 2h elapsed, ETA 30min"
                  },
                  "category": "Software",
                  "subcategory": "Application",
                  "affectedCI": "order-mgmt-prod",
                  "businessService": "Order Management Platform",
                  "assignmentGroup": "Order Platform SRE",
                  "assignedTo": "sarah.chen",
                  "escalationLevel": 1,
                  "customerImpact": "~84,320 users cannot place orders",
                  "revenueImpact": "Estimated $2.3M per hour at current failure rate",
                  "relatedIncidents": ["INC0042892"],
                  "relatedChanges": ["CHG0081234 (order-mgmt v2.4.1 deployment)"],
                  "fullTimeline": [
                    { "time": "13:58", "event": "CHG0081234 - order-mgmt v2.4.1 deployed to production" },
                    { "time": "14:23", "event": "Monitoring alerts fired — error rate exceeded 5%% threshold" },
                    { "time": "14:25", "event": "PagerDuty paged on-call: sarah.chen" },
                    { "time": "14:30", "event": "INC0042891 created in ServiceNow" },
                    { "time": "14:35", "event": "sarah.chen acknowledged — began investigation" },
                    { "time": "16:20", "event": "DB pool exhaustion confirmed by priya.patel" },
                    { "time": "16:25", "event": "Root cause: maxPoolSize config change in v2.4.1" },
                    { "time": "16:28", "event": "Rollback initiated to v2.4.0 via Infiniti pipeline #4532" }
                  ]
                }""";
            default -> """
                {
                  "number": "%s",
                  "status": "NOT_FOUND"
                }""".formatted(incidentNumber);
        };
    }

    @Tool(description = "Get recent ServiceNow change requests (deployments, config changes, maintenance) for a service in the last N hours. Shows what changed recently that may have caused the incident.")
    public String getChangeRequests(String serviceName, int hours) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "lookbackHours": %d,
                  "totalChanges": 1,
                  "changes": [
                    {
                      "number": "CHG0081234",
                      "type": "Standard",
                      "state": "Implemented",
                      "title": "order-mgmt v2.4.1 production deployment",
                      "description": "Deploy order-mgmt v2.4.1 with batch order processing feature and performance optimizations",
                      "implementedAt": "2026-06-11T13:58:00Z",
                      "implementedBy": "mike.rodriguez",
                      "approvedBy": "tom.wilson",
                      "riskLevel": "Medium",
                      "actualRisk": "CRITICAL — config regression caused P1 incident",
                      "rollbackPlan": "Infiniti pipeline rollback to v2.4.0 — ETL 5-10 minutes",
                      "configChanges": [
                        { "parameter": "spring.datasource.hikari.maximum-pool-size", "oldValue": "200", "newValue": "100", "intentional": false, "impact": "CRITICAL" },
                        { "parameter": "feature.batch-order-processing.enabled", "oldValue": "false", "newValue": "true", "intentional": true }
                      ],
                      "postImplementationIssue": "INC0042891 opened 25 minutes after deployment"
                    }
                  ]
                }""".formatted(hours);
            default -> """
                {
                  "service": "%s",
                  "lookbackHours": %d,
                  "totalChanges": 0,
                  "changes": []
                }""".formatted(serviceName, hours);
        };
    }

    @Tool(description = "Get ServiceNow Problem records for a service. Problems are root cause analyses linked to multiple incidents. Shows known recurring issues and permanent fixes.")
    public String getProblemRecords(String serviceName) {
        return """
            {
              "service": "%s",
              "problems": [
                {
                  "number": "PRB0018421",
                  "title": "order-mgmt HikariCP pool configuration drift",
                  "state": "Root Cause Analysis",
                  "severity": "1 - Critical",
                  "openedAt": "2026-06-11T15:00:00Z",
                  "linkedIncidents": ["INC0042891"],
                  "rootCauseDescription": "HikariCP maxPoolSize was accidentally reduced from 200 to 100 in the v2.4.1 deployment configuration. This appears to be a merge conflict resolution error in the application.yml. The team needs to establish configuration review gates in CI/CD to prevent pool size regressions.",
                  "proposedFix": "1. Rollback to v2.4.0 (immediate). 2. Add CI/CD gate to validate pool size config. 3. Implement config drift detection in deployment pipeline.",
                  "knownErrors": [
                    { "id": "KE0042891", "description": "HikariCP pool exhaustion pattern under high load — previous occurrence in March 2024 (resolved by increasing maxPoolSize)" }
                  ]
                }
              ]
            }""".formatted(serviceName);
    }

    @Tool(description = "Create a new ServiceNow incident. Used by the agent to formally log incidents discovered during triaging. Returns the created incident number.")
    public String createIncident(String title, String description, String severity, String serviceName) {
        String incNumber = "INC00" + (42893 + (int)(Math.abs(title.hashCode()) % 100));
        return """
            {
              "status": "CREATED",
              "number": "%s",
              "title": "%s",
              "severity": "%s",
              "affectedService": "%s",
              "state": "New",
              "createdAt": "2026-06-11T16:30:30Z",
              "assignmentGroup": "SRE - Auto-assigned based on service",
              "message": "Incident %s created successfully. PagerDuty alert will be triggered for severity 1 and 2."
            }""".formatted(incNumber, title, severity, serviceName, incNumber);
    }
}
