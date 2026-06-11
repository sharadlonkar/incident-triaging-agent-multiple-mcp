package com.walmart.mcp.pagerduty.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * PagerDuty/xMatters MCP service providing alerting, on-call, and incident management data.
 */
@Service
public class PagerDutyService {

    @Tool(description = "Get all active PagerDuty alerts for a service. Returns triggered and acknowledged alerts with severity, duration, and responder details.")
    public String getActiveAlerts(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "activeAlerts": [
                    {
                      "id": "PD-ALT-8821001",
                      "title": "order-mgmt - HighErrorRate - 34.7%%",
                      "severity": "critical",
                      "status": "acknowledged",
                      "service": "Order Management Platform",
                      "triggeredAt": "2026-06-11T14:24:00Z",
                      "acknowledgedAt": "2026-06-11T14:35:00Z",
                      "acknowledgedBy": "sarah.chen",
                      "duration": "2h 6m",
                      "incidentKey": "order-mgmt-high-error-rate",
                      "body": "error_rate=34.7%%, threshold=5%%, p99_latency=15400ms, SLO_breached=true",
                      "linkedIncident": "INC0042891"
                    },
                    {
                      "id": "PD-ALT-8821002",
                      "title": "order-mgmt - DBConnectionPoolExhausted",
                      "severity": "critical",
                      "status": "acknowledged",
                      "triggeredAt": "2026-06-11T14:23:00Z",
                      "acknowledgedBy": "sarah.chen",
                      "duration": "2h 7m",
                      "body": "HikariPool active=99/100, pending=847, acquisition_timeout_errors=15234"
                    },
                    {
                      "id": "PD-ALT-8821003",
                      "title": "order-mgmt - PodCrashLooping - 3 pods",
                      "severity": "critical",
                      "status": "triggered",
                      "triggeredAt": "2026-06-11T14:45:00Z",
                      "acknowledgedBy": null,
                      "duration": "1h 45m",
                      "body": "3/8 pods CrashLoopBackOff, exitReason=OOMKilled, namespace=order-platform"
                    }
                  ],
                  "resolvedAlerts": 0,
                  "totalActiveAlerts": 3
                }""";
            case "fulfillment-mgmt" -> """
                {
                  "service": "fulfillment-mgmt",
                  "activeAlerts": [
                    {
                      "id": "PD-ALT-8821010",
                      "title": "fulfillment-mgmt - CascadeFailure - 22.3%% error rate",
                      "severity": "warning",
                      "status": "acknowledged",
                      "triggeredAt": "2026-06-11T14:28:00Z",
                      "acknowledgedBy": "james.park",
                      "duration": "2h 2m"
                    }
                  ],
                  "totalActiveAlerts": 1
                }""";
            default -> """
                {
                  "service": "%s",
                  "activeAlerts": [],
                  "totalActiveAlerts": 0,
                  "status": "ALL_CLEAR"
                }""".formatted(serviceName);
        };
    }

    @Tool(description = "Get the current on-call schedule and rotation for an engineering team. Returns who is on-call now, next rotation time, and escalation path.")
    public String getOnCallSchedule(String teamName) {
        return switch (teamName.toLowerCase()) {
            case "order-platform", "order-platform-sre" -> """
                {
                  "team": "Order Platform SRE",
                  "schedule": "Weekly rotation - Monday 09:00 PST",
                  "currentOnCall": {
                    "primary": {
                      "name": "Sarah Chen",
                      "userId": "PD-U-8821",
                      "email": "sarah.chen@walmart.com",
                      "phone": "+1-555-0101",
                      "onCallSince": "2026-06-09T09:00:00Z",
                      "onCallUntil": "2026-06-16T09:00:00Z",
                      "status": "ON_CALL — active on INC0042891"
                    },
                    "secondary": {
                      "name": "Mike Rodriguez",
                      "userId": "PD-U-8822",
                      "phone": "+1-555-0102",
                      "role": "Backup on-call"
                    }
                  },
                  "escalationPolicy": [
                    { "level": 1, "who": "sarah.chen", "notifyAfter": "immediately" },
                    { "level": 2, "who": "mike.rodriguez", "notifyAfter": "15 minutes if not acknowledged" },
                    { "level": 3, "who": "tom.wilson (EM)", "notifyAfter": "30 minutes if not acknowledged" },
                    { "level": 4, "who": "VP Engineering", "notifyAfter": "60 minutes for P1" }
                  ]
                }""";
            case "fulfillment-platform" -> """
                {
                  "team": "Fulfillment Platform",
                  "currentOnCall": {
                    "primary": { "name": "James Park", "phone": "+1-555-0201", "status": "ON_CALL" },
                    "secondary": { "name": "Lisa Wang", "phone": "+1-555-0202" }
                  }
                }""";
            default -> """
                {
                  "team": "%s",
                  "currentOnCall": { "primary": { "name": "General On-Call", "phone": "+1-555-0000" } }
                }""".formatted(teamName);
        };
    }

    @Tool(description = "Get the full incident timeline from PagerDuty showing when alerts fired, who was paged, acknowledgement times, and all status transitions.")
    public String getIncidentTimeline(String incidentId) {
        return switch (incidentId.toUpperCase()) {
            case "INC0042891", "PD-INC-4421" -> """
                {
                  "incidentId": "PD-INC-4421",
                  "serviceNowRef": "INC0042891",
                  "title": "order-mgmt service degradation",
                  "severity": "P1",
                  "timeline": [
                    { "time": "2026-06-11T13:58:00Z", "event": "CHANGE_DEPLOYED", "actor": "infiniti-pipeline", "detail": "order-mgmt v2.4.1 deployed" },
                    { "time": "2026-06-11T14:23:00Z", "event": "ALERT_TRIGGERED", "actor": "prometheus", "detail": "DBConnectionPoolExhausted alert fired" },
                    { "time": "2026-06-11T14:23:30Z", "event": "ALERT_TRIGGERED", "actor": "prometheus", "detail": "HighErrorRate alert fired (34.7%%)" },
                    { "time": "2026-06-11T14:24:00Z", "event": "PAGED", "actor": "pagerduty", "detail": "Primary on-call sarah.chen paged via SMS and app" },
                    { "time": "2026-06-11T14:35:00Z", "event": "ACKNOWLEDGED", "actor": "sarah.chen", "detail": "Incident acknowledged — MTTA: 12 minutes" },
                    { "time": "2026-06-11T14:45:00Z", "event": "ALERT_TRIGGERED", "actor": "kubernetes", "detail": "PodCrashLooping alert — 3 pods OOMKilled" },
                    { "time": "2026-06-11T15:00:00Z", "event": "ESCALATED", "actor": "pagerduty", "detail": "Auto-escalated to L2 (mike.rodriguez) — customer impact growing" },
                    { "time": "2026-06-11T15:30:00Z", "event": "ESCALATED", "actor": "sarah.chen", "detail": "Manually escalated to EM tom.wilson" },
                    { "time": "2026-06-11T16:25:00Z", "event": "ROOT_CAUSE_IDENTIFIED", "actor": "mike.rodriguez", "detail": "HikariCP maxPoolSize reduced in v2.4.1 config" },
                    { "time": "2026-06-11T16:28:00Z", "event": "REMEDIATION_STARTED", "actor": "sarah.chen", "detail": "Rollback initiated — Infiniti pipeline #4532" }
                  ],
                  "mtta": "12 minutes",
                  "timeToRootCause": "2 hours 2 minutes",
                  "status": "IN_REMEDIATION"
                }""";
            default -> """
                {
                  "incidentId": "%s",
                  "status": "NOT_FOUND"
                }""".formatted(incidentId);
        };
    }

    @Tool(description = "Get xMatters notification log showing all pages sent, response times, and escalation events for an incident.")
    public String getNotificationLog(String incidentId) {
        return """
            {
              "incidentId": "%s",
              "notifications": [
                {
                  "timestamp": "2026-06-11T14:24:00Z",
                  "recipient": "sarah.chen",
                  "channel": "SMS",
                  "message": "[PagerDuty P1] order-mgmt - DBConnectionPoolExhausted - Needs immediate attention",
                  "status": "DELIVERED",
                  "responseTime": "11 minutes"
                },
                {
                  "timestamp": "2026-06-11T14:24:05Z",
                  "recipient": "sarah.chen",
                  "channel": "Mobile App",
                  "message": "[PagerDuty P1] order-mgmt - HighErrorRate 34.7%%",
                  "status": "DELIVERED",
                  "responseTime": "11 minutes"
                },
                {
                  "timestamp": "2026-06-11T14:39:00Z",
                  "recipient": "mike.rodriguez",
                  "channel": "SMS",
                  "message": "[PagerDuty P1] ESCALATION: order-mgmt incident not resolved after 15min",
                  "status": "DELIVERED",
                  "responseTime": "3 minutes"
                },
                {
                  "timestamp": "2026-06-11T15:30:00Z",
                  "recipient": "tom.wilson",
                  "channel": "Email + SMS",
                  "message": "[P1 ESCALATION] order-mgmt: 1hr 30min outage, 15k orders affected, $2.3M revenue at risk",
                  "status": "DELIVERED",
                  "responseTime": "5 minutes"
                }
              ]
            }""".formatted(incidentId);
    }

    @Tool(description = "Get PagerDuty service health and mean time to acknowledge (MTTA) and mean time to resolve (MTTR) statistics for a service over the last 30 days.")
    public String getServiceReliabilityMetrics(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "period": "last 30 days",
                  "incidents": {
                    "total": 4,
                    "p1": 1,
                    "p2": 2,
                    "p3": 1
                  },
                  "mtta_minutes": 14.2,
                  "mttr_hours": 1.8,
                  "slaCompliance": "87.5%%",
                  "availability": "99.71%%",
                  "noiseRatio": {
                    "alertsTotal": 142,
                    "actionableAlerts": 18,
                    "noiseAlerts": 124,
                    "noisePercentage": "87.3%%"
                  },
                  "topAlerts": [
                    { "name": "HighErrorRate", "count": 8 },
                    { "name": "DBConnectionPoolHigh", "count": 6 },
                    { "name": "PodCrashLooping", "count": 4 }
                  ],
                  "currentIncident": "INC0042891 — open 2h 7min"
                }""";
            default -> """
                {
                  "service": "%s",
                  "period": "last 30 days",
                  "incidents": { "total": 1, "p1": 0, "p2": 1 },
                  "mtta_minutes": 8.4,
                  "mttr_hours": 0.9,
                  "availability": "99.94%%"
                }""".formatted(serviceName);
        };
    }
}
