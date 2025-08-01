# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy POM first (for Docker layer caching)
COPY pom.xml .

# Clear any cached problematic dependencies and update
RUN mvn dependency:purge-local-repository -B || true

# Try to download dependencies separately first
RUN mvn dependency:resolve -B -U -Dmaven.artifact.threads=1 || true

# Build the plugin directly with force update and fail-at-end
COPY src ./src
RUN mvn clean package -DskipTests -B -U -fae --batch-mode -Dmaven.resolver.transport=wagon

# Runtime stage
FROM atlassian/jira-software:10.3
USER root

# Copy the built plugin
COPY --from=builder /app/target/myPlugin-1.0.0-SNAPSHOT.jar \
     /opt/atlassian/jira/atlassian-jira/WEB-INF/atlassian-bundled-plugins/

# Fix permissions
RUN chown -R jira:jira /opt/atlassian/jira/atlassian-jira/WEB-INF/atlassian-bundled-plugins/

USER jira
EXPOSE 8080