package com.summer.devopsplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
@Configuration
@ConfigurationProperties(prefix = "jenkins")
@Data
public class JenkinsConfig {
    private String url;
    private String username;
    private String apitoken;
}
