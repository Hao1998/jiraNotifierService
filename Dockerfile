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
RUN find /root/.m2/repository -name "*lastUpdated" -delete || true && \
    find /root/.m2/repository -name "_remote.repositories" -delete || true

# Resolve dependencies
RUN mvn dependency:go-offline -B -U || true

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B -U

# Verify the JAR was built and show size
RUN ls -la target/ && \
    test -f target/myPlugin-1.0.0-SNAPSHOT.jar && \
    echo "JAR size: $(du -h target/myPlugin-1.0.0-SNAPSHOT.jar)"

# Runtime Stage - This should be much smaller
FROM atlassian/jira-software:10.3

# Switch to root for file operations
USER root

# Create plugin directory and copy ONLY the JAR
RUN mkdir -p /var/atlassian/application-data/jira/plugins/installed-plugins/
COPY --from=builder /app/target/myPlugin-1.0.0-SNAPSHOT.jar \
     /var/atlassian/application-data/jira/plugins/installed-plugins/

# Fix permissions
RUN chown -R jira:jira /var/atlassian/application-data/jira/

# Switch back to jira user
USER jira

EXPOSE 8080