FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy POM first (for Docker layer caching)
COPY pom.xml .

# Build the plugin directly (no go-offline step)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM atlassian/jira-software:10.3
USER root

# Copy the built plugin
COPY --from=builder /app/target/myPlugin-1.0.0-SNAPSHOT.jar \
     /opt/atlassian/jira/atlassian-jira/WEB-INF/atlassian-bundled-plugins/

USER jira
EXPOSE 8080