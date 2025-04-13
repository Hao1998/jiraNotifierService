package com.atlassian.tutorial.myPlugin.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.tutorial.myPlugin.api.MyPluginComponent;
import com.atlassian.tutorial.myPlugin.config.AwsConfig;
import com.atlassian.tutorial.myPlugin.listener.IssueCreatedResolvedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService({MyPluginComponent.class})
@Named("myPluginComponent")
public class MyPluginComponentImpl implements MyPluginComponent {

    private final ApplicationProperties applicationProperties;


    private final AwsConfig awsConfig;

    private static final Logger log = LoggerFactory.getLogger(IssueCreatedResolvedListener.class);

    @Inject

    public MyPluginComponentImpl(
            @ComponentImport final ApplicationProperties applicationProperties,
            AwsConfig awsConfig) {
        this.applicationProperties = applicationProperties;
        this.awsConfig = awsConfig;
        log.info("Constructing MyPluginComponentImpl");
    }

    public String getName() {
        if (null != applicationProperties) {
            return "myComponent:" + applicationProperties.getDisplayName();
        }

        return "myComponent";
    }
}