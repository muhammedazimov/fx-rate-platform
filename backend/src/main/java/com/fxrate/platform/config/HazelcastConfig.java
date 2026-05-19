package com.fxrate.platform.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Hazelcast.
 * In order to connect to the standalone Docker container, we run Hazelcast in client mode.
 */
@Configuration
public class HazelcastConfig {

    @Value("${app.hazelcast.cluster-name:fx-rate-cluster}")
    private String clusterName;

    @Value("${app.hazelcast.address:localhost:5701}")
    private String address;

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);
        clientConfig.getNetworkConfig().addAddress(address);
        return HazelcastClient.newHazelcastClient(clientConfig);
    }
}
