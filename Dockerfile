# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy POM first (for Docker layer caching)
COPY pom.xml .

# Manually install the problematic commons-httpclient dependency
RUN mkdir -p /root/.m2/repository/commons-httpclient/commons-httpclient/3.1-jenkins-3 && \
    curl -L -o /root/.m2/repository/commons-httpclient/commons-httpclient/3.1-jenkins-3/commons-httpclient-3.1-jenkins-3.jar \
         https://repo.jenkins-ci.org/releases/commons-httpclient/commons-httpclient/3.1-jenkins-3/commons-httpclient-3.1-jenkins-3.jar && \
    curl -L -o /root/.m2/repository/commons-httpclient/commons-httpclient/3.1-jenkins-3/commons-httpclient-3.1-jenkins-3.pom \
         https://repo.jenkins-ci.org/releases/commons-httpclient/commons-httpclient/3.1-jenkins-3/commons-httpclient-3.1-jenkins-3.pom || true

# Clear any failed download markers
RUN find /root/.m2/repository -name "*lastUpdated" -delete || true
RUN find /root/.m2/repository -name "_remote.repositories" -delete || true

# Try to resolve dependencies
RUN mvn dependency:resolve -B -U || true

# Build the plugin
COPY src ./src
RUN mvn clean package -DskipTests -B -U

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