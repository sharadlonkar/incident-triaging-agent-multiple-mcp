# Multi-stage build for any module in this multi-module Maven project.
# Usage: docker build --build-arg MODULE=incident-agent -t walmart/incident-agent .
#        docker build --build-arg MODULE=mcp-mms-server  -t walmart/mcp-mms-server .

ARG MODULE

# ─── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

# Copy everything; Maven needs the parent POM and all module sources
COPY pom.xml .
COPY incident-agent/pom.xml       incident-agent/
COPY mcp-mms-server/pom.xml       mcp-mms-server/
COPY mcp-dynatrace-server/pom.xml mcp-dynatrace-server/
COPY mcp-openobserve-server/pom.xml mcp-openobserve-server/
COPY mcp-kubernetes-server/pom.xml mcp-kubernetes-server/
COPY mcp-slack-server/pom.xml     mcp-slack-server/
COPY mcp-servicenow-server/pom.xml mcp-servicenow-server/
COPY mcp-pagerduty-server/pom.xml  mcp-pagerduty-server/
COPY mcp-infiniti-server/pom.xml   mcp-infiniti-server/
COPY mcp-jira-server/pom.xml       mcp-jira-server/

# Resolve dependencies (cached layer)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q

# Copy source
COPY . .

ARG MODULE
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl ${MODULE} -am -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ARG MODULE
COPY --from=builder /build/${MODULE}/target/*.jar app.jar

# JVM flags: container-aware heap sizing, GC logging
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar $0 $@"]
