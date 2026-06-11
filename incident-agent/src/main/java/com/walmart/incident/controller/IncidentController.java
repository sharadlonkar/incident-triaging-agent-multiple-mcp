package com.walmart.incident.controller;

import com.walmart.incident.model.IncidentRequest;
import com.walmart.incident.model.IncidentReport;
import com.walmart.incident.service.IncidentTriagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/incident")
public class IncidentController {

    private static final Logger log = LoggerFactory.getLogger(IncidentController.class);

    private final IncidentTriagingService triagingService;

    public IncidentController(IncidentTriagingService triagingService) {
        this.triagingService = triagingService;
    }

    /**
     * Triage an incident by order ID or service name.
     *
     * Example request bodies:
     * { "orderId": "WMT-98765" }
     * { "serviceName": "order-mgmt" }
     * { "orderId": "WMT-98765", "serviceName": "order-mgmt", "additionalContext": "Customer reporting 503 errors" }
     */
    @PostMapping("/triage")
    public ResponseEntity<IncidentReport> triageIncident(@RequestBody IncidentRequest request) {
        log.info("Received triage request: {}", request.getPrimaryContext());

        if (!request.hasOrderId() && !request.hasServiceName()) {
            return ResponseEntity.badRequest().build();
        }

        IncidentReport report = triagingService.triageIncident(request);
        return ResponseEntity.ok(report);
    }

    /**
     * Quick triage by order ID (GET for convenience).
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<IncidentReport> triageByOrderId(@PathVariable String orderId) {
        log.info("Quick triage for order: {}", orderId);
        IncidentRequest request = new IncidentRequest(orderId, null, null);
        return ResponseEntity.ok(triagingService.triageIncident(request));
    }

    /**
     * Quick triage by service name (GET for convenience).
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<IncidentReport> triageByService(@PathVariable String serviceName) {
        log.info("Quick triage for service: {}", serviceName);
        IncidentRequest request = new IncidentRequest(null, serviceName, null);
        return ResponseEntity.ok(triagingService.triageIncident(request));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "incident-triaging-agent",
            "description", "Walmart AI Incident Triaging Agent with 9 MCP servers"
        ));
    }
}
