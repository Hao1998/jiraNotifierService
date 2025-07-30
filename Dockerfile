# ==========================================
# Build Stage: Compile Jira Plugin with Maven
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code and build with Maven directly
COPY src ./src

# Build the plugin using standard Maven (your pom.xml has jira-maven-plugin)
RUN mvn clean package -DskipTests -B

# Verify build output
RUN echo "Build verification:" && \
    ls -la target/ && \
    find target/ -name "*.jar" -type f

# ==========================================
# Runtime Stage: Jira with Plugin
# ==========================================
FROM atlassian/jira-software:10.3

USER root

# Install AWS CLI
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" \
    && unzip awscliv2.zip \
    && ./aws/install \
    && rm -rf awscliv2.zip aws \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy the built plugin JAR from builder stage
COPY --from=builder /app/target/myPlugin-1.0.0-SNAPSHOT.jar \
     /opt/atlassian/jira/atlassian-jira/WEB-INF/atlassian-bundled-plugins/

# Set proper permissions
RUN chown -R jira:jira /opt/atlassian/jira/atlassian-jira/WEB-INF/atlassian-bundled-plugins/

USER jira

EXPOSE 8080