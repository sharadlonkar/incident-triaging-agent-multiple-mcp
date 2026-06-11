# Walmart Incident Triaging AI Agent — Multiple MCP

> **Proof of Concept** — An AI-powered incident triaging agent built with Spring AI that autonomously queries 9 enterprise observability and ITSM systems via the Model Context Protocol (MCP), then synthesises all findings into an executive summary for engineering leadership.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        INCIDENT TRIAGING AGENT                          │
│                         Spring AI  ·  Port 8080                         │
│                                                                         │
│   ┌──────────────┐      ┌──────────────────────────────────────────┐   │
│   │  REST API    │      │              ChatClient                  │   │
│   │              │      │  (claude-sonnet-4-5  ·  Anthropic API)   │   │
│   │ POST /triage │─────▶│                                          │   │
│   │ GET  /order  │      │  ┌────────────────────────────────────┐  │   │
│   │ GET  /service│◀─────│  │    MCP Tool Callback Registry      │  │   │
│   └──────────────┘      │  │  (45 tools across 9 MCP servers)   │  │   │
│                          │  └────────────────┬───────────────────┘  │   │
│                          └───────────────────┼──────────────────────┘   │
└──────────────────────────────────────────────┼──────────────────────────┘
                                               │  SSE (HTTP)
                           ┌───────────────────┼───────────────────┐
                           │      MCP CLIENT   │   CONNECTIONS     │
                           └───────────────────┼───────────────────┘
         ┌─────────────────┬──────────────┬────┴──────────┬─────────────────┐
         │                 │              │               │                 │
         ▼                 ▼              ▼               ▼                 ▼
┌────────────────┐ ┌──────────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────┐
│  MMS Server    │ │  Dynatrace   │ │OpenObserv│ │ Kubernetes │ │   Slack      │
│  :8081         │ │  Server:8082 │ │ Server   │ │ Server     │ │   Server     │
│                │ │              │ │ :8083    │ │ :8084      │ │   :8085      │
│ Prometheus /   │ │ APM · OneAgt │ │Logs·Splnk│ │Pods·Events │ │ Channels ·  │
│ Grafana        │ │ Davis AI RCA │ │Exceptions│ │HPA·Rollout │ │ On-Call      │
│                │ │              │ │          │ │            │ │              │
│ 5 tools        │ │ 5 tools      │ │ 5 tools  │ │ 5 tools    │ │ 5 tools      │
│ 5 prompts      │ │ 5 prompts    │ │ 5 prompts│ │ 5 prompts  │ │ 5 prompts    │
│ 2 resources    │ │ 2 resources  │ │2 resource│ │ 2 resources│ │ 2 resources  │
└────────────────┘ └──────────────┘ └──────────┘ └────────────┘ └──────────────┘

         ┌─────────────────┬──────────────┬────────────────┬─────────────────┐
         │                 │              │                │                 │
         ▼                 ▼              ▼                ▼                 ▼
┌────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  ServiceNow    │ │  PagerDuty   │ │   Infiniti   │ │    Jira      │
│  Server :8086  │ │  Server:8087 │ │  Server:8088 │ │  Server:8089 │
│                │ │              │ │              │ │              │
│ Incidents ·    │ │ Alerts ·     │ │ CI/CD Gates· │ │ Bugs ·       │
│ Changes ·      │ │ On-Call ·    │ │ Pipelines ·  │ │ Sprints ·    │
│ Problems · SLA │ │ MTTA / MTTR  │ │ Rollback     │ │ Order issues │
│                │ │              │ │              │ │              │
│ 5 tools        │ │ 5 tools      │ │ 5 tools      │ │ 5 tools      │
│ 5 prompts      │ │ 5 prompts    │ │ 5 prompts    │ │ 5 prompts    │
│ 2 resources    │ │ 2 resources  │ │ 2 resources  │ │ 2 resources  │
└────────────────┘ └──────────────┘ └──────────────┘ └──────────────┘

                           ┌───────────────────────┐
                           │   ANTHROPIC CLAUDE API │
                           │   claude-sonnet-4-5    │
                           │   (LLM reasoning core) │
                           └───────────────────────┘
```

---

## Proof of Concept Overview

### What It Demonstrates

This PoC shows how a **single AI agent** can replace a manual multi-tool incident investigation — what would typically take an SRE 30–60 minutes correlating data across 9 different dashboards — down to a single API call that returns a structured executive summary in under 2 minutes.

### Problem Being Solved

During a production incident at scale (Walmart processes millions of orders/day), the first 30 minutes are typically lost to:
- Switching between Dynatrace, Grafana, Splunk, Kubernetes CLI, Slack, ServiceNow
- Manually correlating timestamps across tools
- Identifying which team owns the problem and who is on-call
- Drafting status updates for leadership

This agent automates the entire investigation and synthesis step.

---

## Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| AI Framework | Spring AI 1.0.0 | Enterprise Java ecosystem, native MCP support |
| LLM | Anthropic Claude Sonnet 4.5 | Strong reasoning over large tool outputs |
| MCP Transport | SSE (HTTP) | Works across separate JVM processes, firewall-friendly |
| Tool Design | `@Tool` annotation on service methods | Zero boilerplate, auto-generates JSON schema |
| Tool Providers | `MethodToolCallbackProvider` bean per server | Spring AI MCP auto-wires all 9 into `ChatClient` |
| Data | Hardcoded, consistent scenario | Reproducible demo, no external dependencies |

---

## MCP Servers — Tool Inventory

| Server | Port | Tools (5 each) |
|---|---|---|
| **MMS** (Prometheus/Grafana) | 8081 | `getServiceLatency` · `getErrorRate` · `getDatabaseMetrics` · `getInfraMetrics` · `getActiveAlerts` |
| **Dynatrace** | 8082 | `getApplicationHealth` · `getAnomalies` · `getDependencyMap` · `getRootCauseAnalysis` · `getDeploymentImpact` |
| **OpenObserve/Splunk** | 8083 | `searchLogs` · `getErrorLogs` · `getOrderAuditTrail` · `getLogVolumeTrend` · `getExceptionSummary` |
| **Kubernetes** | 8084 | `getPodStatus` · `getNodeHealth` · `getDeploymentStatus` · `getClusterEvents` · `getScalingStatus` |
| **Slack** | 8085 | `getChannelMessages` · `searchMessages` · `getOnCallTeam` · `getActiveIncidentDiscussions` · `postMessage` |
| **ServiceNow** | 8086 | `getIncidents` · `getIncidentDetails` · `getChangeRequests` · `getProblemRecords` · `createIncident` |
| **PagerDuty/xMatters** | 8087 | `getActiveAlerts` · `getOnCallSchedule` · `getIncidentTimeline` · `getNotificationLog` · `getServiceReliabilityMetrics` |
| **Infiniti CI/CD** | 8088 | `getPipelineStatus` · `getDeploymentHistory` · `getGateResults` · `getRollbackOptions` · `getPullRequestDetails` |
| **Jira** | 8089 | `getOpenBugs` · `getRecentIssues` · `getSprintStatus` · `getLinkedIssues` · `createBug` |

**Total: 45 tools · 45 prompts · 18 resources**

Each server also exposes:
- **5 MCP prompts** — pre-built investigation prompts for common triage workflows
- **2 MCP resources** — tool descriptions and data schemas/indexes

---

## Hardcoded Demo Scenario

All 9 MCP servers contain consistent data describing the same live incident:

```
INCIDENT:   INC0042891  ·  Severity P1  ·  Open 2h 7m
ORDER:      WMT-98765   ·  Customer CUST-442198  ·  $284.99
SERVICES:   order-mgmt (PRIMARY)  →  fulfillment-mgmt (CASCADE)

ROOT CAUSE: order-mgmt v2.4.1 deployment at 13:58 UTC reduced
            HikariCP maxPoolSize from 200 → 100 (config regression
            introduced during PR-4821 merge conflict resolution)

IMPACT:     ~15,000 orders stuck  ·  ~84,320 users affected
            Error rate: 34.7%  ·  p99 latency: 15,400ms (vs 320ms SLO)
            Revenue at risk: ~$2.3M/hour

K8S:        3/8 order-mgmt pods OOMKilled  ·  HPA at max (8/8)
            wmt-prod-node-07: 96.6% memory pressure

REMEDIATION: Rollback pipeline-run-4532 in progress → v2.4.0
             ETA to recovery: ~10 minutes
```

This scenario is reflected consistently across MMS alerts, Dynatrace anomalies, OpenObserve exception traces, Kubernetes events, Slack war-room messages, ServiceNow work notes, PagerDuty timeline, Infiniti deployment history, and Jira bug tickets.

---

## Tech Stack

```
Language:       Java 21
Framework:      Spring Boot 3.3.0
AI Layer:       Spring AI 1.0.0
LLM:            Anthropic Claude (claude-sonnet-4-5)
MCP Protocol:   Model Context Protocol 1.0 (SSE transport)
Build:          Maven (multi-module)
Packaging:      Docker (multi-stage, parameterised ARG MODULE)
Orchestration:  Docker Compose
```

---

## Project Structure

```
incident-triaging-agent-multiple-mcp/
├── pom.xml                              ← Parent POM (BOM for Spring AI + Boot)
│
├── incident-agent/                      ← Main AI Agent  (port 8080)
│   └── src/main/java/com/walmart/incident/
│       ├── IncidentTriagingAgentApplication.java
│       ├── config/AgentConfig.java      ← ChatClient wired with all 45 MCP tools
│       ├── controller/IncidentController.java
│       ├── service/IncidentTriagingService.java
│       └── model/{IncidentRequest,IncidentReport}.java
│
├── mcp-mms-server/                      ← MCP Server: Metrics  (port 8081)
│   └── src/main/java/com/walmart/mcp/mms/
│       ├── service/MmsService.java      ← 5 @Tool methods
│       └── config/McpServerConfig.java  ← 5 prompts + 2 resources
│
│   [same structure repeated for each of the 8 remaining MCP servers]
│
├── Dockerfile                           ← Multi-stage, --build-arg MODULE=…
├── docker-compose.yml                   ← All 10 services + health checks
├── start-all.sh                         ← Build + start all locally
├── stop-all.sh
└── .env.example                         ← ANTHROPIC_API_KEY template
```

---

## Running the PoC

### Prerequisites
- Java 21+
- Maven 3.9+
- `ANTHROPIC_API_KEY` from [console.anthropic.com](https://console.anthropic.com)

### Option A — Local (start-all.sh)

```bash
git clone <repo>
cd incident-triaging-agent-multiple-mcp

export ANTHROPIC_API_KEY=sk-ant-your-key-here
./start-all.sh
```

The script builds all modules, starts the 9 MCP servers, waits for each to pass health checks, then starts the agent.

### Option B — Docker Compose

```bash
cp .env.example .env
# Edit .env and set ANTHROPIC_API_KEY

docker compose up --build
```

---

## API Reference

### Triage by Order ID
```bash
curl http://localhost:8080/api/v1/incident/order/WMT-98765
```

### Triage by Service Name
```bash
curl http://localhost:8080/api/v1/incident/service/order-mgmt
```
Valid service names: `order-mgmt` · `fulfillment-mgmt` · `pre-pick-mgmt` · `pick-mgmt` · `post-pick-mgmt` · `delivery-mgmt`

### Full Triage (POST with context)
```bash
curl -X POST http://localhost:8080/api/v1/incident/triage \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "WMT-98765",
    "serviceName": "order-mgmt",
    "additionalContext": "Customer support flooded with 503 errors since 2:30pm"
  }'
```

### Response Shape
```json
{
  "requestId": "A3F2E9C1",
  "request": { "orderId": "WMT-98765", "serviceName": "order-mgmt" },
  "executiveSummary": "## 🚨 Incident Summary\n...",
  "status": "COMPLETED",
  "analysisTimeMs": 87432
}
```

---

## Agent Investigation Strategy

The agent follows a structured investigation playbook encoded in its system prompt:

```
1. Slack (#incidents-p1)       → What do engineers already know?
2. ServiceNow                  → Formal incident record + recent changes
3. PagerDuty                   → Alert timeline + who is responding
4. MMS (Prometheus/Grafana)    → Full alert inventory + metrics
5. Dynatrace                   → Root cause analysis + anomaly chain
6. OpenObserve                 → Log evidence + stack traces
7. Kubernetes                  → Infrastructure health + pod status
8. Infiniti (CI/CD)            → Deployment history + rollback options
9. Jira                        → Known bugs + customer impact
10. Synthesis                  → Executive summary with facts + recommendations
```

---

## Executive Summary Format

The agent produces a structured report in the following sections:

| Section | Content |
|---|---|
| 🚨 Incident Summary | ID, severity, duration, services affected |
| 📊 Impact Assessment | Orders impacted, user count, revenue at risk, SLO status |
| 🔍 Root Cause | Specific version/config/component that failed, evidence per tool |
| 📅 Timeline | Chronological key events with timestamps |
| ✅ Actions Taken | What engineers have already done |
| 🔧 Recommended Actions | Prioritised immediate + follow-up + prevention |
| 👥 Responders | On-call engineers, escalation status |

---

## Limitations (PoC Scope)

- **Hardcoded data only** — no live connections to real Prometheus, Dynatrace, etc.
- **Single incident scenario** — data is tuned to the `WMT-98765` / `order-mgmt` incident
- **No authentication** — MCP server endpoints are unsecured
- **No streaming** — the REST API is synchronous (can take 60–120s for full triage)
- **No persistence** — triage reports are returned but not stored

---

## Extending the PoC

To connect a real data source, replace the hardcoded `return` statements in any `*Service.java` with actual HTTP calls to your observability APIs. The MCP protocol, tool definitions, prompts, and agent logic remain unchanged.

```java
// Before (hardcoded)
@Tool(description = "Get service latency metrics")
public String getServiceLatency(String serviceName, int minutes) {
    return """{ "p99_ms": 15400 ... }""";
}

// After (real Prometheus)
@Tool(description = "Get service latency metrics")
public String getServiceLatency(String serviceName, int minutes) {
    return prometheusClient.query(
        "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket{service='%s'}[%dm]))"
            .formatted(serviceName, minutes)
    );
}
```
