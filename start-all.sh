#!/bin/bash
# Walmart Incident Triaging Agent - Start All Services
# =====================================================
# Starts all 9 MCP servers and the main incident agent.
# Each MCP server runs in the background; the agent starts last.

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$PROJECT_DIR/logs"

mkdir -p "$LOG_DIR"

check_port() {
    local port=$1
    if lsof -Pi ":$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠ Port $port already in use — skipping${NC}"
        return 1
    fi
    return 0
}

wait_for_health() {
    local name=$1
    local port=$2
    local max_wait=60
    local elapsed=0
    echo -n "  Waiting for $name to be ready"
    while [ $elapsed -lt $max_wait ]; do
        if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e " ${GREEN}✓${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        elapsed=$((elapsed + 2))
    done
    echo -e " ${RED}✗ TIMEOUT${NC}"
    return 1
}

# ── Check API key ─────────────────────────────────────────────────
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo -e "${RED}ERROR: ANTHROPIC_API_KEY environment variable is not set${NC}"
    echo "Export it with: export ANTHROPIC_API_KEY=your_key_here"
    exit 1
fi

# ── Build all modules ──────────────────────────────────────────────
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Walmart Incident Triaging Agent Startup  ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""
echo "Building all Maven modules..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# ── MCP Server definitions: name, port, module ────────────────────
declare -a SERVERS=(
    "mcp-mms-server:8081:mcp-mms-server"
    "mcp-dynatrace-server:8082:mcp-dynatrace-server"
    "mcp-openobserve-server:8083:mcp-openobserve-server"
    "mcp-kubernetes-server:8084:mcp-kubernetes-server"
    "mcp-slack-server:8085:mcp-slack-server"
    "mcp-servicenow-server:8086:mcp-servicenow-server"
    "mcp-pagerduty-server:8087:mcp-pagerduty-server"
    "mcp-infiniti-server:8088:mcp-infiniti-server"
    "mcp-jira-server:8089:mcp-jira-server"
)

echo "Starting MCP servers..."
for server_def in "${SERVERS[@]}"; do
    IFS=':' read -r name port module <<< "$server_def"
    JAR="$PROJECT_DIR/$module/target/$module-1.0.0-SNAPSHOT.jar"

    if check_port "$port"; then
        echo -e "  ${BLUE}→${NC} Starting $name on port $port"
        java -jar "$JAR" > "$LOG_DIR/$name.log" 2>&1 &
        echo $! > "$LOG_DIR/$name.pid"
    fi
done

echo ""
echo "Waiting for all MCP servers to be healthy..."
ALL_HEALTHY=true
for server_def in "${SERVERS[@]}"; do
    IFS=':' read -r name port module <<< "$server_def"
    if ! wait_for_health "$name" "$port"; then
        ALL_HEALTHY=false
        echo -e "${RED}  ERROR: $name failed to start. Check $LOG_DIR/$name.log${NC}"
    fi
done

if [ "$ALL_HEALTHY" = false ]; then
    echo -e "${RED}Some MCP servers failed to start. Aborting.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}All 9 MCP servers are healthy!${NC}"
echo ""

# ── Start the main agent ──────────────────────────────────────────
echo "Starting Incident Triaging Agent on port 8080..."
AGENT_JAR="$PROJECT_DIR/incident-agent/target/incident-agent-1.0.0-SNAPSHOT.jar"

if check_port "8080"; then
    java -jar "$AGENT_JAR" \
        --spring.ai.anthropic.api-key="$ANTHROPIC_API_KEY" \
        > "$LOG_DIR/incident-agent.log" 2>&1 &
    echo $! > "$LOG_DIR/incident-agent.pid"
    wait_for_health "incident-agent" "8080"
fi

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✓ All services started successfully!                 ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
echo "Service endpoints:"
echo "  Incident Agent:          http://localhost:8080"
echo "  MMS Server:              http://localhost:8081"
echo "  Dynatrace Server:        http://localhost:8082"
echo "  OpenObserve Server:      http://localhost:8083"
echo "  Kubernetes Server:       http://localhost:8084"
echo "  Slack Server:            http://localhost:8085"
echo "  ServiceNow Server:       http://localhost:8086"
echo "  PagerDuty Server:        http://localhost:8087"
echo "  Infiniti Server:         http://localhost:8088"
echo "  Jira Server:             http://localhost:8089"
echo ""
echo "Example requests:"
echo ""
echo "  # Triage by order ID:"
echo "  curl http://localhost:8080/api/v1/incident/order/WMT-98765"
echo ""
echo "  # Triage by service name:"
echo "  curl http://localhost:8080/api/v1/incident/service/order-mgmt"
echo ""
echo "  # Full triage with context (POST):"
echo '  curl -X POST http://localhost:8080/api/v1/incident/triage \'
echo '    -H "Content-Type: application/json" \'
echo '    -d '"'"'{"orderId":"WMT-98765","serviceName":"order-mgmt","additionalContext":"Customer reporting 503 errors"}'"'"
echo ""
echo "Logs: $LOG_DIR/"
echo ""
echo "To stop all services: ./stop-all.sh"
