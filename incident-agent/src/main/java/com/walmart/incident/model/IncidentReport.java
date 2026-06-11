package com.walmart.incident.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IncidentReport(
    @JsonProperty("requestId")
    String requestId,

    @JsonProperty("request")
    IncidentRequest request,

    @JsonProperty("executiveSummary")
    String executiveSummary,

    @JsonProperty("status")
    String status,

    @JsonProperty("analysisTimeMs")
    long analysisTimeMs
) {}
