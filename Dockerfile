FROM atlassian/jira-software:latest

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

# Copy your plugin JAR
COPY target/myPlugin-1.0.0-SNAPSHOT.jar /opt/atlassian/jira/atlassian-jira/WEB-INF/lib/

# Switch back to jira user
USER jira

EXPOSE 8080