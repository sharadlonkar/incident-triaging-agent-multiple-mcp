package com.walmart.incident.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IncidentRequest(
    @JsonProperty("orderId")
    String orderId,

    @JsonProperty("serviceName")
    String serviceName,

    @JsonProperty("additionalContext")
    String additionalContext
) {
    public boolean hasOrderId() {
        return orderId != null && !orderId.isBlank();
    }

    public boolean hasServiceName() {
        return serviceName != null && !serviceName.isBlank();
    }

    public String getPrimaryContext() {
        if (hasOrderId()) return "Order ID: " + orderId;
        if (hasServiceName()) return "Service: " + serviceName;
        return "General incident triage";
    }
}
