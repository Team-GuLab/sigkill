package com.gulab.sigkillserver;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SigkillServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SigkillServerApplication.class, args);
    }
}
