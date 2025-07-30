# ==========================================
# Build Stage: Compile Jira Plugin
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Install required packages first
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Atlassian SDK with better error handling
RUN echo "Downloading Atlassian SDK..." && \
    wget --timeout=60 --tries=3 -v \
    https://maven.atlassian.com/public/com/atlassian/amps/atlassian-plugin-sdk/9.3.0/atlassian-plugin-sdk-9.3.0.tar.gz \
    -O atlassian-plugin-sdk-9.3.0.tar.gz && \
    echo "Download completed. Extracting..." && \
    tar -xzf atlassian-plugin-sdk-9.3.0.tar.gz && \
    mv atlassian-plugin-sdk-9.3.0 /opt/atlassian-plugin-sdk && \
    rm atlassian-plugin-sdk-9.3.0.tar.gz && \
    echo "SDK installation completed"

# Add AMPS to PATH
ENV PATH="/opt/atlassian-plugin-sdk/bin:${PATH}"

WORKDIR /app

# Copy and cache dependencies first
COPY pom.xml .
RUN echo "Running dependency resolution..." && \
    atlas-mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN echo "Building plugin..." && \
    atlas-mvn clean package -DskipTests -B

# Verify build output
RUN ls -la target/ && \
    find target/ -name "*.jar" -type f

# ==========================================
# Runtime Stage: Jira with Plugin
# ==========================================
FROM atlassian/jira-software:10.3

# Switch to root for installations
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

# Switch back to jira user
USER jira

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8080/status || exit 1

EXPOSE 8080