#!/bin/bash
# Stop all Walmart Incident Triaging Agent services

LOG_DIR="$(cd "$(dirname "$0")" && pwd)/logs"

SERVICES=(
    "incident-agent"
    "mcp-mms-server"
    "mcp-dynatrace-server"
    "mcp-openobserve-server"
    "mcp-kubernetes-server"
    "mcp-slack-server"
    "mcp-servicenow-server"
    "mcp-pagerduty-server"
    "mcp-infiniti-server"
    "mcp-jira-server"
)

echo "Stopping all services..."
for service in "${SERVICES[@]}"; do
    PID_FILE="$LOG_DIR/$service.pid"
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID"
            echo "  Stopped $service (PID $PID)"
        fi
        rm -f "$PID_FILE"
    fi
done
echo "Done."
