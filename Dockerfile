# ==========================================
# Build Stage: Compile Jira Plugin
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Try multiple sources for Atlassian SDK
RUN echo "Attempting to download Atlassian SDK..." && \
    (wget --timeout=60 --tries=2 \
    https://maven.atlassian.com/public/com/atlassian/amps/atlassian-plugin-sdk/9.3.0/atlassian-plugin-sdk-9.3.0.tar.gz \
    -O atlassian-plugin-sdk-9.3.0.tar.gz || \
    curl -L --max-time 60 --retry 2 \
    https://maven.atlassian.com/public/com/atlassian/amps/atlassian-plugin-sdk/9.3.0/atlassian-plugin-sdk-9.3.0.tar.gz \
    -o atlassian-plugin-sdk-9.3.0.tar.gz) && \
    echo "Download completed. File size: $(ls -lh atlassian-plugin-sdk-9.3.0.tar.gz)" && \
    tar -xzf atlassian-plugin-sdk-9.3.0.tar.gz && \
    mv atlassian-plugin-sdk-9.3.0 /opt/atlassian-plugin-sdk && \
    rm atlassian-plugin-sdk-9.3.0.tar.gz && \
    ls -la /opt/atlassian-plugin-sdk/bin/ && \
    echo "SDK installation completed"

# Add AMPS to PATH
ENV PATH="/opt/atlassian-plugin-sdk/bin:${PATH}"

WORKDIR /app

# Verify atlas-mvn is available
RUN which atlas-mvn && atlas-mvn --version

# Copy and cache dependencies first
COPY pom.xml .
RUN echo "Running dependency resolution..." && \
    atlas-mvn dependency:go-offline -B -X

# Copy source code and build
COPY src ./src
RUN echo "Building plugin..." && \
    atlas-mvn clean package -DskipTests -B -X

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