
package com.aerosaga.backend.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalClientConfig {

    @Value("${temporal.service.target}")
    private String target;

    @Value("${temporal.namespace:default}")
    private String namespace;

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs() {
        WorkflowServiceStubsOptions options =
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(target)
                        .build();

        return WorkflowServiceStubs.newServiceStubs(options);
    }

    @Bean
    public WorkflowClient workflowClient(
            WorkflowServiceStubs serviceStubs) {

        WorkflowClientOptions options =
                WorkflowClientOptions.newBuilder()
                        .setNamespace(namespace)
                        .build();

        return WorkflowClient.newInstance(
                serviceStubs, options);
    }
}
