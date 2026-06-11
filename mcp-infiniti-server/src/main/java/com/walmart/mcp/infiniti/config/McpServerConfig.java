package com.walmart.mcp.infiniti.config;

import com.walmart.mcp.infiniti.service.InfinitiService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider infinitiTools(InfinitiService infinitiService) {
        return MethodToolCallbackProvider.builder().toolObjects(infinitiService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> infinitiPrompts() {
        var deploymentAuditPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("infiniti-deployment-audit",
                "Audit recent deployments to identify if a change deployment caused the current incident",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to audit", true),
                         new McpSchema.PromptArgument("count", "Number of recent deployments to review", true))),
            req -> new McpSchema.GetPromptResult("Deployment audit",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Audit the last " + req.arguments().get("count") + " deployments for '" +
                        req.arguments().get("serviceName") + "'. " +
                        "Identify which deployment introduced the breaking change, " +
                        "what specific code/config changed, and confirm the last stable version for rollback."
                    ))))
        );

        var rollbackDecisionPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("infiniti-rollback-decision",
                "Evaluate rollback options and recommend the safest rollback path with command",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to roll back", true))),
            req -> new McpSchema.GetPromptResult("Rollback decision",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Evaluate rollback options for '" + req.arguments().get("serviceName") +
                        "'. Recommend the safest and fastest rollback target. " +
                        "Provide the exact Infiniti pipeline command and estimated recovery time. " +
                        "Note any risk with the rollback (e.g. data migrations, feature flags)."
                    ))))
        );

        var gateAnalysisPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("infiniti-gate-gap-analysis",
                "Analyze CI/CD gates to find which quality check should have caught the regression but didn't",
                List.of(new McpSchema.PromptArgument("pipelineRunId", "Pipeline run to analyze", true))),
            req -> new McpSchema.GetPromptResult("Gate gap analysis",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Analyze CI/CD gates for pipeline run '" + req.arguments().get("pipelineRunId") +
                        "'. Identify which gate should have caught the regression but didn't. " +
                        "Recommend specific gate improvements to prevent similar incidents."
                    ))))
        );

        var prReviewPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("infiniti-pr-review",
                "Review the pull request changes to understand what was introduced in the problematic deployment",
                List.of(new McpSchema.PromptArgument("prNumber", "PR number from deployment", true))),
            req -> new McpSchema.GetPromptResult("PR review",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Review PR '" + req.arguments().get("prNumber") + "' to understand what code changes " +
                        "were introduced in the problematic deployment. Identify the specific change that " +
                        "caused the regression, who reviewed it, and what the review process missed."
                    ))))
        );

        var deploymentFrequencyPrompt = new McpServerFeatures.SyncPromptRegistration(
            new McpSchema.Prompt("infiniti-deployment-risk",
                "Assess deployment risk based on change frequency and DORA metrics",
                List.of(new McpSchema.PromptArgument("serviceName", "Service to assess", true))),
            req -> new McpSchema.GetPromptResult("Deployment risk",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent(
                        "Assess the deployment risk profile for '" + req.arguments().get("serviceName") +
                        "' based on deployment frequency, failure rate, and recovery time. " +
                        "Are deployments happening too frequently? Is the rollback process fast enough? " +
                        "Provide DORA metric assessment."
                    ))))
        );

        return List.of(deploymentAuditPrompt, rollbackDecisionPrompt, gateAnalysisPrompt, prReviewPrompt, deploymentFrequencyPrompt);
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> infinitiResources() {
        var toolsDescription = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("infiniti://resources/tools-description",
                "Infiniti Pipeline Tools Description",
                "Description of all Infiniti CI/CD pipeline MCP tools",
                "text/plain", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("infiniti://resources/tools-description", "text/plain",
                    """
                    Infiniti CI/CD Pipeline MCP Server Tools
                    =========================================

                    Walmart's internal CI/CD platform (Infiniti) built on Tekton + ArgoCD.
                    Manages deployments for 500+ microservices across 6 environments.

                    AVAILABLE TOOLS:

                    1. getPipelineStatus(serviceName)
                       - Current pipeline status and last deployment
                       - Gate results for the last run
                       - Deployment health across all environments
                       - Active rollback status if in progress

                    2. getDeploymentHistory(serviceName, count)
                       - Last N deployments with versions and timestamps
                       - Who deployed and which PR triggered it
                       - Post-deploy stability (did incidents follow?)
                       - Last stable version identification

                    3. getGateResults(pipelineRunId)
                       - Detailed gate results: unit tests, integration, security, SonarQube
                       - Code coverage percentages
                       - Security CVE findings and waivers
                       - Config validation gaps (what was missed)

                    4. getRollbackOptions(serviceName)
                       - Available rollback targets with risk assessment
                       - Exact rollback commands (kubectl and Infiniti)
                       - Estimated rollback time
                       - Active rollback progress if underway

                    5. getPullRequestDetails(prNumber)
                       - Files changed, lines added/removed
                       - Critical changes (config, schema, feature flags)
                       - Reviewer list and approval history
                       - Post-merge incident correlation

                    ENVIRONMENTS: dev → staging → prod-canary → prod
                    DEPLOYMENT FREQUENCY: Multiple times per day per service
                    ROLLBACK SLO: < 10 minutes
                    """)
            ))
        );

        var pipelineConfig = new McpServerFeatures.SyncResourceRegistration(
            new McpSchema.Resource("infiniti://resources/pipeline-gates",
                "CI/CD Pipeline Gates Configuration",
                "Standard gate configuration for Walmart production deployments",
                "application/json", null),
            req -> new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents("infiniti://resources/pipeline-gates", "application/json",
                    """
                    {
                      "standardGates": [
                        { "name": "unit-tests", "required": true, "threshold": "100%% pass, >85%% coverage" },
                        { "name": "integration-tests", "required": true, "threshold": "100%% pass" },
                        { "name": "security-scan", "required": true, "blockOn": "critical or high CVEs" },
                        { "name": "sonar-quality-gate", "required": true, "threshold": "Grade A or B" },
                        { "name": "config-validation", "required": true, "note": "Schema-based, may miss non-schema configs" },
                        { "name": "load-test", "required": false, "requiredFor": "high-risk changes" },
                        { "name": "canary-analysis", "required": true, "duration": "15 minutes", "threshold": "error rate <1%%" }
                      ],
                      "riskLevels": {
                        "LOW": "Config-only, docs",
                        "MEDIUM": "New features, refactoring",
                        "HIGH": "DB schema, auth, payments — requires load test + extended canary"
                      }
                    }
                    """)
            ))
        );

        return List.of(toolsDescription, pipelineConfig);
    }
}
