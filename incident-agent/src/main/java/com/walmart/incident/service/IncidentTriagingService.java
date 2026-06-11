package com.walmart.incident.service;

import com.walmart.incident.model.IncidentRequest;
import com.walmart.incident.model.IncidentReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IncidentTriagingService {

    private static final Logger log = LoggerFactory.getLogger(IncidentTriagingService.class);

    private final ChatClient chatClient;

    public IncidentTriagingService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public IncidentReport triageIncident(IncidentRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[{}] Starting incident triage for: {}", requestId, request.getPrimaryContext());

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);

        String summary = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[{}] Triage completed in {}ms", requestId, elapsed);

        return new IncidentReport(requestId, request, summary, "COMPLETED", elapsed);
    }

    private String buildSystemPrompt() {
        return """
            You are the Walmart Incident Triaging AI Agent — an expert Site Reliability Engineer
            with deep knowledge of distributed systems, observability, and incident response.

            You have access to the following MCP servers (tools) to gather data:
            - MMS (Prometheus/Grafana): service latency, error rates, DB metrics, infra metrics, active alerts
            - Dynatrace: application health, anomalies, dependency maps, root cause analysis, deployment impact
            - OpenObserve/Splunk: log search, error logs, order audit trails, exception summaries
            - Kubernetes: pod status, node health, deployment rollout, cluster events, HPA scaling
            - Slack: incident channel messages, on-call team contacts, war room discussions
            - ServiceNow: incident records, change requests, problem records, SLA tracking
            - PagerDuty: active alerts, on-call schedules, incident timelines, MTTA/MTTR
            - Infiniti (CI/CD): pipeline status, deployment history, gate results, rollback options
            - Jira: open bugs, sprint status, order-linked issues, recent work items

            INVESTIGATION STRATEGY:
            1. Check Slack first — engineers may have already identified the root cause
            2. Get ServiceNow incident and recent change requests to establish context
            3. Check PagerDuty for alert timeline and who is responding
            4. Pull MMS alerts to get the full picture of what is firing
            5. Investigate Dynatrace for root cause analysis and anomaly intelligence
            6. Analyze OpenObserve logs and exception patterns
            7. Check Kubernetes pod status and recent cluster events
            8. Check Infiniti for recent deployments and rollback options
            9. Cross-reference Jira for known bugs and customer impact
            10. Synthesize all findings into an executive summary

            EXECUTIVE SUMMARY FORMAT:
            Generate a structured executive summary with these sections:

            ## 🚨 Incident Summary
            - Incident ID, severity, duration, affected services

            ## 📊 Impact Assessment
            - Quantified customer impact (orders affected, users impacted)
            - Revenue impact estimate
            - SLO/SLA status

            ## 🔍 Root Cause
            - Primary root cause (be specific — version, config parameter, value)
            - Contributing factors
            - Evidence from each data source

            ## 📅 Timeline
            - Key events in chronological order

            ## ✅ Actions Taken
            - What the engineering team has already done

            ## 🔧 Recommended Actions
            - Immediate actions (in priority order)
            - Short-term follow-up
            - Prevention measures

            ## 👥 Responders
            - Who is on-call and responding
            - Escalation status

            Always use specific facts, numbers, and timestamps from the data. Never speculate without evidence.
            Be concise but comprehensive. Target ~500 words for the executive summary.
            """;
    }

    private String buildUserPrompt(IncidentRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Triage the following incident and generate an executive summary:\n\n");

        if (request.hasOrderId()) {
            prompt.append("ORDER ID: ").append(request.orderId()).append("\n");
            prompt.append("An order with this ID is experiencing issues. Trace it through all services.\n");
            prompt.append("Check the order audit trail, identify where it got stuck, and correlate with service health.\n\n");
        }

        if (request.hasServiceName()) {
            prompt.append("AFFECTED SERVICE: ").append(request.serviceName()).append("\n");
            prompt.append("This service has reported errors or degradation. Investigate thoroughly.\n");
            prompt.append("Valid services: order-mgmt, fulfillment-mgmt, pre-pick-mgmt, pick-mgmt, post-pick-mgmt, delivery-mgmt\n\n");
        }

        if (request.additionalContext() != null && !request.additionalContext().isBlank()) {
            prompt.append("ADDITIONAL CONTEXT: ").append(request.additionalContext()).append("\n\n");
        }

        prompt.append("""
            Please:
            1. Use ALL available MCP server tools to gather comprehensive data
            2. Start by checking Slack for existing human investigation context
            3. Get the ServiceNow incident record and change history
            4. Analyze metrics from MMS and Dynatrace
            5. Review logs from OpenObserve for exception patterns
            6. Check Kubernetes infrastructure health
            7. Review CI/CD deployment history from Infiniti
            8. Check Jira for related bugs
            9. Synthesize all findings into a factual executive summary

            Focus on FACTS from the data. Include specific numbers, times, versions, and evidence.
            The summary will be presented to engineering leadership.
            """);

        return prompt.toString();
    }
}
