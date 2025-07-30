# ==========================================
# Build Stage: Compile Jira Plugin
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# First, try with updated POM.xml using standard Maven
COPY pom.xml .

# Clean Maven cache and try with new repositories
RUN mvn dependency:purge-local-repository -B || true

# Download dependencies using updated repositories
RUN mvn dependency:go-offline -B -U

# Copy source code and build
COPY src ./src

# Build using standard Maven (your pom.xml has the jira-maven-plugin configured)
RUN mvn clean package -DskipTests -B -U

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