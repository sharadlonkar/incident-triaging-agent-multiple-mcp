package com.walmart.mcp.infiniti.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Infiniti CI/CD Pipeline MCP service providing deployment and pipeline data.
 * Walmart's internal CI/CD platform built on top of Tekton/ArgoCD.
 */
@Service
public class InfinitiService {

    @Tool(description = "Get the current pipeline status and last deployment details for a service. Returns build/deploy status, gate results, and deployment health.")
    public String getPipelineStatus(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "pipeline": "order-mgmt-deploy-pipeline",
                  "lastRun": {
                    "id": "pipeline-run-4521",
                    "version": "v2.4.1",
                    "status": "SUCCEEDED_WITH_ISSUES",
                    "triggeredAt": "2026-06-11T13:45:00Z",
                    "completedAt": "2026-06-11T13:58:00Z",
                    "duration": "13 minutes",
                    "triggeredBy": "mike.rodriguez",
                    "gitCommit": "a3f2e9c",
                    "gitBranch": "release/2.4.1",
                    "prNumber": "PR-4821"
                  },
                  "gates": {
                    "unitTests": "PASSED (98.4%% coverage)",
                    "integrationTests": "PASSED",
                    "securityScan": "PASSED (2 low CVEs, waived)",
                    "sonarQube": "PASSED (quality gate: A)",
                    "loadTest": "SKIPPED — not required for this change type",
                    "configValidation": "PASSED (no schema violations detected)",
                    "dryRun": "PASSED"
                  },
                  "deploymentEnvironments": [
                    { "env": "dev", "status": "DEPLOYED", "deployedAt": "2026-06-11T12:10:00Z" },
                    { "env": "staging", "status": "DEPLOYED", "deployedAt": "2026-06-11T12:45:00Z", "stagingTestResult": "PASSED" },
                    { "env": "prod", "status": "DEPLOYED_DEGRADED", "deployedAt": "2026-06-11T13:58:00Z", "issue": "P1 incident INC0042891" }
                  ],
                  "rollbackPipeline": {
                    "status": "IN_PROGRESS",
                    "id": "pipeline-run-4532",
                    "rollbackTo": "v2.4.0",
                    "startedAt": "2026-06-11T16:28:00Z",
                    "estimatedCompletion": "2026-06-11T16:38:00Z"
                  },
                  "knownIssue": "CONFIG_REGRESSION: spring.datasource.hikari.maximum-pool-size accidentally set to 100 (was 200)"
                }""";
            default -> """
                {
                  "service": "%s",
                  "lastRun": {
                    "status": "SUCCEEDED",
                    "version": "latest",
                    "triggeredAt": "2026-06-10T08:00:00Z"
                  },
                  "gates": { "all": "PASSED" }
                }""".formatted(serviceName);
        };
    }

    @Tool(description = "Get deployment history for a service showing the last N deployments with versions, timestamps, and who triggered them. Helps identify which deployment caused an issue.")
    public String getDeploymentHistory(String serviceName, int count) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "deploymentCount": %d,
                  "deployments": [
                    {
                      "version": "v2.4.1",
                      "pipelineRunId": "pipeline-run-4521",
                      "deployedAt": "2026-06-11T13:58:00Z",
                      "deployedBy": "mike.rodriguez",
                      "gitCommit": "a3f2e9c",
                      "prNumber": "PR-4821",
                      "prTitle": "feat: batch order processing + performance config tuning",
                      "status": "DEPLOYED_DEGRADED",
                      "changesSummary": "Added batch order endpoint, CHANGED HikariCP maxPoolSize 200→100",
                      "postDeployIssue": "P1 incident INC0042891 — 25 min after deploy"
                    },
                    {
                      "version": "v2.4.0",
                      "pipelineRunId": "pipeline-run-4498",
                      "deployedAt": "2026-06-09T10:15:00Z",
                      "deployedBy": "sarah.chen",
                      "gitCommit": "b8c1d4f",
                      "prNumber": "PR-4790",
                      "prTitle": "fix: order status webhook retry logic",
                      "status": "STABLE",
                      "changesSummary": "Fixed webhook retry with exponential backoff",
                      "postDeployIssue": null
                    },
                    {
                      "version": "v2.3.9",
                      "pipelineRunId": "pipeline-run-4441",
                      "deployedAt": "2026-06-05T14:30:00Z",
                      "deployedBy": "alex.johnson",
                      "status": "STABLE"
                    },
                    {
                      "version": "v2.3.8",
                      "deployedAt": "2026-05-28T11:00:00Z",
                      "deployedBy": "mike.rodriguez",
                      "status": "STABLE"
                    },
                    {
                      "version": "v2.3.7",
                      "deployedAt": "2026-05-20T09:30:00Z",
                      "deployedBy": "sarah.chen",
                      "status": "STABLE"
                    }
                  ],
                  "changeFrequency": "2-3 deployments per week",
                  "lastStableVersion": "v2.4.0",
                  "rollbackRecommendation": "Rollback to v2.4.0 (last stable, no issues post-deploy)"
                }""".formatted(count);
            default -> """
                {
                  "service": "%s",
                  "deploymentCount": %d,
                  "deployments": [
                    { "version": "latest", "status": "STABLE", "deployedAt": "2026-06-10T08:00:00Z" }
                  ]
                }""".formatted(serviceName, count);
        };
    }

    @Tool(description = "Get CI/CD gate results and quality metrics for a specific pipeline run. Shows unit test results, code coverage, security scan findings, and quality gate status.")
    public String getGateResults(String pipelineRunId) {
        return switch (pipelineRunId.toLowerCase()) {
            case "pipeline-run-4521" -> """
                {
                  "pipelineRunId": "pipeline-run-4521",
                  "service": "order-mgmt",
                  "version": "v2.4.1",
                  "gateResults": {
                    "unitTests": {
                      "status": "PASSED",
                      "total": 1842,
                      "passed": 1842,
                      "failed": 0,
                      "coverage": "98.4%%",
                      "duration": "4m 12s"
                    },
                    "integrationTests": {
                      "status": "PASSED",
                      "total": 342,
                      "passed": 342,
                      "failed": 0,
                      "duration": "8m 45s"
                    },
                    "securityScan": {
                      "status": "PASSED_WITH_WAIVERS",
                      "critical": 0,
                      "high": 0,
                      "medium": 0,
                      "low": 2,
                      "waivedVulnerabilities": ["CVE-2024-11081 (Jackson, low severity)", "CVE-2024-10923 (logback, low severity)"]
                    },
                    "sonarQube": {
                      "status": "PASSED",
                      "qualityGate": "A",
                      "bugs": 0,
                      "vulnerabilities": 0,
                      "codeSmells": 12,
                      "technicalDebt": "45 minutes",
                      "duplications": "1.2%%"
                    },
                    "configValidation": {
                      "status": "PASSED",
                      "note": "MISSED: maxPoolSize change not flagged by schema validator (pool size not in validation schema)",
                      "gap": "Config schema does not validate HikariCP parameters — improvement needed"
                    },
                    "loadTest": {
                      "status": "SKIPPED",
                      "reason": "Change classified as medium risk — load test not mandatory"
                    }
                  },
                  "overallStatus": "PASSED",
                  "gapIdentified": "Config validation gate did not catch HikariCP maxPoolSize change from 200 to 100"
                }""";
            default -> """
                {
                  "pipelineRunId": "%s",
                  "status": "NOT_FOUND"
                }""".formatted(pipelineRunId);
        };
    }

    @Tool(description = "Get rollback options and initiate a rollback for a service. Returns available rollback targets and estimated rollback time.")
    public String getRollbackOptions(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "order-mgmt" -> """
                {
                  "service": "order-mgmt",
                  "currentVersion": "v2.4.1",
                  "currentStatus": "DEGRADED — P1 incident active",
                  "rollbackOptions": [
                    {
                      "targetVersion": "v2.4.0",
                      "recommendation": "RECOMMENDED",
                      "reason": "Last stable version, no post-deploy issues, HikariCP maxPoolSize=200",
                      "deployedAt": "2026-06-09T10:15:00Z",
                      "k8sRevision": 11,
                      "rollbackMethod": "kubectl rollout undo deployment/order-mgmt -n order-platform --to-revision=11",
                      "infinitiPipelineCommand": "infiniti rollback order-mgmt --to-version v2.4.0 --env prod",
                      "estimatedTime": "3-5 minutes",
                      "riskLevel": "LOW"
                    },
                    {
                      "targetVersion": "v2.3.9",
                      "reason": "Two versions back — safe but skips v2.4.0 webhook fix",
                      "estimatedTime": "3-5 minutes",
                      "riskLevel": "LOW-MEDIUM"
                    }
                  ],
                  "activeRollback": {
                    "status": "IN_PROGRESS",
                    "pipelineRunId": "pipeline-run-4532",
                    "startedAt": "2026-06-11T16:28:00Z",
                    "initiatedBy": "sarah.chen",
                    "targetVersion": "v2.4.0",
                    "estimatedCompletion": "2026-06-11T16:38:00Z",
                    "currentStep": "Deploying v2.4.0 to prod (2/4 steps complete)"
                  }
                }""";
            default -> """
                {
                  "service": "%s",
                  "rollbackOptions": [
                    { "targetVersion": "previous", "estimatedTime": "3-5 minutes", "riskLevel": "LOW" }
                  ]
                }""".formatted(serviceName);
        };
    }

    @Tool(description = "Get the PR (pull request) details and code diff summary for a specific pipeline run. Useful for identifying what code changes were included in a problematic deployment.")
    public String getPullRequestDetails(String prNumber) {
        return switch (prNumber.toUpperCase()) {
            case "PR-4821" -> """
                {
                  "prNumber": "PR-4821",
                  "title": "feat: batch order processing + performance config tuning",
                  "author": "mike.rodriguez",
                  "reviewers": ["sarah.chen", "alex.johnson"],
                  "approvedBy": ["sarah.chen"],
                  "mergedAt": "2026-06-11T13:42:00Z",
                  "mergedBy": "mike.rodriguez",
                  "baseBranch": "main",
                  "headBranch": "feature/batch-order-processing",
                  "filesChanged": 23,
                  "linesAdded": 842,
                  "linesRemoved": 124,
                  "criticalChanges": [
                    {
                      "file": "src/main/resources/application.yml",
                      "change": "spring.datasource.hikari.maximum-pool-size: 200 → 100",
                      "reason": "Accidentally changed during merge conflict resolution",
                      "reviewNote": "NOT CAUGHT IN REVIEW — config file changes not highlighted in diff"
                    },
                    {
                      "file": "src/main/java/com/walmart/ordermgmt/controller/OrderController.java",
                      "change": "Added POST /api/v2/orders/batch endpoint",
                      "intentional": true
                    }
                  ],
                  "testResults": "All 1842 unit tests passing, 342 integration tests passing",
                  "riskAssessment": "Medium — new feature, config changes not reviewed thoroughly",
                  "postMergeIssue": "P1 incident INC0042891 traced to config change in this PR"
                }""";
            default -> """
                {
                  "prNumber": "%s",
                  "status": "NOT_FOUND"
                }""".formatted(prNumber);
        };
    }
}
