package com.walmart.mcp.jira.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Jira MCP service providing issue tracking, sprint, and bug data.
 */
@Service
public class JiraService {

    @Tool(description = "Get open bug reports for a service in Jira. Returns bugs filtered by service component, sorted by priority. Helps identify known bugs that may be contributing to the incident.")
    public String getOpenBugs(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "project": "ORDER",
                  "openBugs": [
                    {
                      "key": "ORDER-4821",
                      "title": "HikariCP connection pool exhaustion under sustained high load",
                      "priority": "Critical",
                      "status": "In Progress",
                      "assignee": "mike.rodriguez",
                      "reporter": "monitoring-team",
                      "created": "2026-06-11T15:00:00Z",
                      "labels": ["production-incident", "database", "performance", "INC0042891"],
                      "description": "P1 incident INC0042891: order-mgmt v2.4.1 inadvertently reduced HikariCP maxPoolSize from 200 to 100, causing pool exhaustion under production load. Rollback in progress.",
                      "linkedIncident": "INC0042891",
                      "linkedPR": "PR-4821",
                      "sprint": "Sprint 47"
                    },
                    {
                      "key": "ORDER-4756",
                      "title": "Order status webhook occasionally fails to deliver after retry exhaustion",
                      "priority": "Medium",
                      "status": "To Do",
                      "assignee": "sarah.chen",
                      "created": "2026-06-08T10:30:00Z",
                      "labels": ["reliability", "webhook"]
                    },
                    {
                      "key": "ORDER-4698",
                      "title": "Batch order import times out for orders >500 items",
                      "priority": "High",
                      "status": "In Progress",
                      "assignee": "alex.johnson",
                      "created": "2026-06-01T14:00:00Z",
                      "labels": ["performance", "batch"]
                    }
                  ],
                  "totalOpenBugs": 3
                }""";
            case "fulfillment-mgmt" -> """
                {
                  "service": "fulfillment-mgmt",
                  "project": "FULFILL",
                  "openBugs": [
                    {
                      "key": "FULFILL-2341",
                      "title": "fulfillment-mgmt cascade errors from order-mgmt outage",
                      "priority": "High",
                      "status": "In Progress",
                      "assignee": "james.park",
                      "created": "2026-06-11T15:30:00Z",
                      "labels": ["cascade-failure", "dependency", "INC0042891"]
                    }
                  ],
                  "totalOpenBugs": 1
                }""";
            default -> """
                {
                  "service": "%s",
                  "openBugs": [],
                  "totalOpenBugs": 0
                }""".formatted(serviceName);
        };
    }

    @Tool(description = "Get recently created Jira issues for a service in the last N days. Includes bugs, tasks, and tech debt items that may relate to the current incident.")
    public String getRecentIssues(String serviceName, int days) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "lookbackDays": %d,
                  "totalIssues": 8,
                  "issues": [
                    {
                      "key": "ORDER-4821",
                      "type": "Bug",
                      "title": "HikariCP pool exhaustion — P1 incident INC0042891",
                      "priority": "Critical",
                      "createdDaysAgo": 0,
                      "status": "In Progress"
                    },
                    {
                      "key": "ORDER-4820",
                      "type": "Task",
                      "title": "Batch order processing feature — v2.4.1 implementation",
                      "priority": "High",
                      "createdDaysAgo": 3,
                      "status": "Done",
                      "linkedPR": "PR-4821"
                    },
                    {
                      "key": "ORDER-4818",
                      "type": "Technical Debt",
                      "title": "Review and document HikariCP tuning parameters",
                      "priority": "Medium",
                      "createdDaysAgo": 5,
                      "status": "To Do",
                      "note": "This task would have caught the config issue if done before v2.4.1"
                    },
                    {
                      "key": "ORDER-4800",
                      "type": "Story",
                      "title": "Implement DB connection pool monitoring alerts",
                      "priority": "High",
                      "createdDaysAgo": 10,
                      "status": "In Progress",
                      "note": "Alert ORDER-4800 was being worked on — had it been done, pool exhaustion would have been caught earlier"
                    }
                  ]
                }""".formatted(days);
            default -> """
                {
                  "service": "%s",
                  "lookbackDays": %d,
                  "totalIssues": 2,
                  "issues": []
                }""".formatted(serviceName, days);
        };
    }

    @Tool(description = "Get the current sprint status for an engineering team showing velocity, completed work, and in-progress items. Helps understand team capacity during an incident.")
    public String getSprintStatus(String teamName) {
        return switch (teamName.toLowerCase()) {
            case "order-platform", "order-mgmt" -> """
                {
                  "team": "Order Platform",
                  "currentSprint": {
                    "name": "Sprint 47",
                    "startDate": "2026-06-08",
                    "endDate": "2026-06-21",
                    "daysRemaining": 10,
                    "status": "ACTIVE"
                  },
                  "velocity": {
                    "committed": 84,
                    "completed": 31,
                    "remaining": 53,
                    "burndown": "ON_TRACK before incident"
                  },
                  "currentFocus": "P1 incident INC0042891 — all engineers redirected",
                  "inProgressItems": [
                    { "key": "ORDER-4821", "title": "P1: HikariCP pool exhaustion fix", "points": 5 },
                    { "key": "ORDER-4698", "title": "Batch order timeout fix", "points": 8 },
                    { "key": "ORDER-4800", "title": "DB pool monitoring alerts", "points": 5 }
                  ],
                  "teamCapacity": {
                    "total": 5,
                    "availableForIncident": 5,
                    "onLeave": 0
                  }
                }""";
            default -> """
                {
                  "team": "%s",
                  "currentSprint": { "name": "Sprint 47", "status": "ACTIVE", "daysRemaining": 10 },
                  "velocity": { "committed": 60, "completed": 28 }
                }""".formatted(teamName);
        };
    }

    @Tool(description = "Find all Jira issues linked to a specific order ID or incident. Returns any bugs, tasks, or customer escalations related to the given order.")
    public String getLinkedIssues(String orderId) {
        return switch (orderId.toUpperCase()) {
            case "WMT-98765" -> """
                {
                  "orderId": "WMT-98765",
                  "linkedIssues": [
                    {
                      "key": "ORDER-4822",
                      "type": "Customer Escalation",
                      "title": "Customer CUST-442198 unable to complete order WMT-98765 - payment pre-authorized",
                      "priority": "High",
                      "status": "New",
                      "createdAt": "2026-06-11T15:45:00Z",
                      "reportedBy": "customer-support-team",
                      "note": "Customer has been waiting 2+ hours. Payment pre-auth expires in 1.5 hours."
                    },
                    {
                      "key": "ORDER-4821",
                      "type": "Bug",
                      "title": "HikariCP pool exhaustion — root cause for WMT-98765 failure",
                      "priority": "Critical",
                      "status": "In Progress"
                    }
                  ],
                  "customerImpact": {
                    "orderValue": "$284.99",
                    "customerSince": "2019",
                    "loyaltyTier": "Walmart+",
                    "paymentPreAuth": { "valid": true, "expiresAt": "2026-06-11T18:21:31Z", "amount": "$284.99" },
                    "recommendedAction": "Order can be retried after service recovery. Contact customer with apology and compensation voucher."
                  }
                }""";
            default -> """
                {
                  "orderId": "%s",
                  "linkedIssues": [],
                  "note": "No Jira issues found for this order"
                }""".formatted(orderId);
        };
    }

    @Tool(description = "Create a new Jira bug report for an issue discovered during incident triaging. Returns the created issue key.")
    public String createBug(String title, String description, String serviceName, String priority) {
        String projectKey = switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> "ORDER";
            case "fulfillment-mgmt", "pre-pick-mgmt", "pick-mgmt", "post-pick-mgmt" -> "FULFILL";
            case "delivery-mgmt" -> "DLVRY";
            default -> "PLATFORM";
        };
        int issueNum = 4823 + (int)(Math.abs(title.hashCode()) % 100);
        return """
            {
              "status": "CREATED",
              "key": "%s-%d",
              "title": "%s",
              "priority": "%s",
              "service": "%s",
              "project": "%s",
              "type": "Bug",
              "status_field": "New",
              "createdAt": "2026-06-11T16:30:30Z",
              "url": "https://walmart.atlassian.net/browse/%s-%d"
            }""".formatted(projectKey, issueNum, title, priority, serviceName, projectKey, projectKey, issueNum);
    }
}
