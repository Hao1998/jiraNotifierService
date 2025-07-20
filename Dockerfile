FROM adoptopenjdk/openjdk8:latest

# Set environment variables
ENV JIRA_HOME     /var/atlassian/jira
ENV JIRA_INSTALL  /opt/atlassian/jira
ENV JIRA_VERSION  9.4.0

# Install required packages
RUN apt-get update \
    && apt-get install -y curl wget \
    && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p "${JIRA_HOME}" \
    && mkdir -p "${JIRA_INSTALL}"

# Download and extract JIRA
RUN curl -L --silent https://product-downloads.atlassian.com/software/jira/downloads/atlassian-jira-software-${JIRA_VERSION}.tar.gz | tar -xz --strip-components=1 -C "${JIRA_INSTALL}"

# Copy your plugin JAR
COPY target/myPlugin-1.0.0-SNAPSHOT.jar "${JIRA_INSTALL}/atlassian-jira/WEB-INF/lib/"

# Set directory permissions
RUN chown -R daemon:daemon "${JIRA_HOME}" \
    && chown -R daemon:daemon "${JIRA_INSTALL}"

# Expose default ports
EXPOSE 8080

# Set working directory
WORKDIR ${JIRA_INSTALL}

# Run Jira as daemon user
USER daemon

# Start Jira
CMD ["./bin/start-jira.sh", "-fg"]