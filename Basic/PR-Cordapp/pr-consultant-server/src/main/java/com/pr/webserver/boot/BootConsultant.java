package com.pr.webserver.boot;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.WebApplicationType.SERVLET;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */


@SpringBootApplication(scanBasePackages = {"com.pr.webserver.*","com.pr.server.common"})
public class BootConsultant {
    /**
     * Starts Consultant Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication.run(BootConsultant.class, args);
    }
}