package com.walmart.mcp.slack.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Slack MCP service providing team communication and incident discussion data.
 * Simulates Slack API access for incident response channels.
 */
@Service
public class SlackService {

    @Tool(description = "Get recent messages from a Slack channel in the last X minutes. Supports channels: #incidents-p1, #order-platform-alerts, #fulfillment-platform-alerts, #oncall-engineering")
    public String getChannelMessages(String channel, int minutes) {
        return switch (channel.toLowerCase().replace("#", "")) {
            case "incidents-p1" -> """
                {
                  "channel": "#incidents-p1",
                  "timeRange": "%d minutes",
                  "messageCount": 47,
                  "messages": [
                    {
                      "timestamp": "2026-06-11T16:29:00Z",
                      "user": "sarah.chen (SRE Lead)",
                      "message": "Still investigating root cause. DB pool exhaustion confirmed on order-mgmt-db. Working on rollback decision.",
                      "reactions": ["🔍 3", "👍 5"]
                    },
                    {
                      "timestamp": "2026-06-11T16:25:00Z",
                      "user": "mike.rodriguez (Platform Eng)",
                      "message": "@here order-mgmt v2.4.1 deployment reduced HikariCP maxPoolSize from 200 to 100. This is likely the root cause. Rollback in progress via Infiniti pipeline.",
                      "reactions": ["⚡ 8", "🎯 4"]
                    },
                    {
                      "timestamp": "2026-06-11T16:20:00Z",
                      "user": "priya.patel (DB Admin)",
                      "message": "Confirmed: order-mgmt-db connection pool is at 99/100 with 847 pending acquisitions. This is the bottleneck.",
                      "reactions": ["✅ 6"]
                    },
                    {
                      "timestamp": "2026-06-11T16:15:00Z",
                      "user": "tom.wilson (Engineering Manager)",
                      "message": "Business impact update: ~15,000 orders stuck, customer support flooded. Need ETA on fix ASAP.",
                      "reactions": ["⚠️ 2"]
                    },
                    {
                      "timestamp": "2026-06-11T14:30:00Z",
                      "user": "pagerduty-bot",
                      "message": "🚨 P1 INCIDENT CREATED: INC0042891 - order-mgmt availability drop. On-call engineer paged: sarah.chen",
                      "reactions": []
                    },
                    {
                      "timestamp": "2026-06-11T14:25:00Z",
                      "user": "monitoring-bot",
                      "message": "🔴 CRITICAL: order-mgmt error rate 34.7%% (threshold: 5%%). Firing for 2 minutes. Cascade to fulfillment-mgmt detected.",
                      "reactions": ["🚨 3"]
                    }
                  ]
                }""".formatted(minutes);
            case "order-platform-alerts" -> """
                {
                  "channel": "#order-platform-alerts",
                  "timeRange": "%d minutes",
                  "messageCount": 128,
                  "messages": [
                    {
                      "timestamp": "2026-06-11T16:29:55Z",
                      "user": "grafana-bot",
                      "message": "[CRITICAL] order-mgmt p99 latency: 15400ms (SLO: 500ms). 3 pods in CrashLoopBackOff."
                    },
                    {
                      "timestamp": "2026-06-11T16:28:00Z",
                      "user": "grafana-bot",
                      "message": "[CRITICAL] HikariCP pool exhausted: 99/100 active, 847 pending"
                    },
                    {
                      "timestamp": "2026-06-11T16:25:00Z",
                      "user": "sarah.chen",
                      "message": "Confirmed: root cause is maxPoolSize config change in v2.4.1. Rollback initiated."
                    }
                  ]
                }""".formatted(minutes);
            default -> """
                {
                  "channel": "#%s",
                  "timeRange": "%d minutes",
                  "messageCount": 5,
                  "messages": [
                    { "timestamp": "2026-06-11T16:00:00Z", "user": "system", "message": "No significant activity" }
                  ]
                }""".formatted(channel, minutes);
        };
    }

    @Tool(description = "Search across all Slack channels for messages containing a specific keyword or phrase in the last X minutes. Useful for finding discussions about a specific error, order ID, or service.")
    public String searchMessages(String keyword, int minutes) {
        if (keyword.toLowerCase().contains("order-mgmt") || keyword.toLowerCase().contains("hikari")
                || keyword.toLowerCase().contains("pool") || keyword.toLowerCase().contains("wmt-98765")) {
            return """
                {
                  "keyword": "%s",
                  "timeRange": "%d minutes",
                  "totalMatches": 89,
                  "channels": [
                    {
                      "channel": "#incidents-p1",
                      "matchCount": 34,
                      "topMatch": "HikariCP pool exhausted — order-mgmt v2.4.1 config change reduced maxPoolSize 200→100"
                    },
                    {
                      "channel": "#order-platform-alerts",
                      "matchCount": 42,
                      "topMatch": "[CRITICAL] HikariCP pool exhausted: 99/100 active, 847 pending"
                    },
                    {
                      "channel": "#engineering-oncall",
                      "matchCount": 13,
                      "topMatch": "sarah.chen: Confirmed DB pool exhaustion. Rolling back order-mgmt to v2.4.0"
                    }
                  ]
                }""".formatted(keyword, minutes);
        }
        return """
            {
              "keyword": "%s",
              "timeRange": "%d minutes",
              "totalMatches": 3,
              "channels": []
            }""".formatted(keyword, minutes);
    }

    @Tool(description = "Get the current on-call engineer and team details for a specific service team. Returns primary and secondary on-call contacts with phone and Slack handles.")
    public String getOnCallTeam(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt", "order-platform" -> """
                {
                  "service": "order-mgmt",
                  "team": "Order Platform Engineering",
                  "oncallSchedule": "PagerDuty rotation - weekly",
                  "primary": {
                    "name": "Sarah Chen",
                    "role": "SRE Lead",
                    "slack": "@sarah.chen",
                    "phone": "+1-555-0101",
                    "pagerdutyId": "PD-U-8821",
                    "timezone": "PST",
                    "status": "ACTIVE — acknowledged incident INC0042891"
                  },
                  "secondary": {
                    "name": "Mike Rodriguez",
                    "role": "Platform Engineer",
                    "slack": "@mike.rodriguez",
                    "phone": "+1-555-0102",
                    "pagerdutyId": "PD-U-8822"
                  },
                  "manager": {
                    "name": "Tom Wilson",
                    "role": "Engineering Manager",
                    "slack": "@tom.wilson",
                    "escalationLevel": "L3"
                  },
                  "slackChannels": ["#order-platform-alerts", "#incidents-p1"]
                }""";
            case "fulfillment-mgmt", "fulfillment-platform" -> """
                {
                  "service": "fulfillment-mgmt",
                  "team": "Fulfillment Platform Engineering",
                  "primary": {
                    "name": "James Park",
                    "role": "Senior Engineer",
                    "slack": "@james.park",
                    "phone": "+1-555-0201",
                    "status": "ACTIVE — monitoring cascade from order-mgmt"
                  },
                  "secondary": {
                    "name": "Lisa Wang",
                    "role": "Engineer",
                    "slack": "@lisa.wang",
                    "phone": "+1-555-0202"
                  },
                  "slackChannels": ["#fulfillment-platform-alerts", "#incidents-p1"]
                }""";
            default -> """
                {
                  "service": "%s",
                  "primary": { "name": "On-Call Engineer", "slack": "@oncall", "phone": "+1-555-0000" }
                }""".formatted(serviceName);
        };
    }

    @Tool(description = "Get active incident war room discussions across all incident channels. Returns a summary of all P1/P2 incidents currently being discussed in Slack.")
    public String getActiveIncidentDiscussions() {
        return """
            {
              "timestamp": "2026-06-11T16:30:00Z",
              "activeIncidents": [
                {
                  "incidentId": "INC0042891",
                  "title": "order-mgmt service degradation — DB pool exhaustion",
                  "severity": "P1",
                  "channel": "#incidents-p1",
                  "openedAt": "2026-06-11T14:30:00Z",
                  "duration": "2 hours",
                  "participantCount": 12,
                  "lastActivity": "2 minutes ago",
                  "currentStatus": "Root cause identified. Rollback in progress.",
                  "keyUpdates": [
                    "14:30 — Incident declared, on-call paged",
                    "16:20 — DB pool exhaustion confirmed (priya.patel)",
                    "16:25 — Root cause identified: v2.4.1 config change (mike.rodriguez)",
                    "16:28 — Rollback initiated via Infiniti pipeline",
                    "16:29 — ETA 5-10 min for rollback completion"
                  ],
                  "nextAction": "Monitor order-mgmt error rate post-rollback"
                }
              ]
            }""";
    }

    @Tool(description = "Post a message to a Slack channel. Used by the incident agent to send the executive summary or status updates. Channel must be one of: #incidents-p1, #order-platform-alerts, #engineering-leadership")
    public String postMessage(String channel, String message) {
        return """
            {
              "status": "SENT",
              "channel": "#%s",
              "timestamp": "2026-06-11T16:30:30Z",
              "messagePreview": "%s",
              "messageTs": "1749656230.024800",
              "permalink": "https://walmart.slack.com/archives/C04XYZ/%s"
            }""".formatted(
                channel.replace("#", ""),
                message.length() > 100 ? message.substring(0, 97) + "..." : message,
                "p1749656230024800"
        );
    }
}
