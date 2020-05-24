package com.pr.server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@Configuration
@PropertySource("classpath:common.properties")
public class CommonConfig {
    @Bean
    public ObjectMapper cordaObjectMapper() {
        return JacksonSupport.createNonRpcMapper();
    }
}
