package com.pr.webserver.boot;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.WebApplicationType.SERVLET;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@SpringBootApplication(scanBasePackages = {"com.pr.webserver.*","com.pr.server.common"})
public class BootUniversity {
    /**
     * Starts University Spring Boot application.
     */
   /* public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BootUniversity.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(SERVLET);
        app.run(args);
    }*/
    public static void main(String[] args) {
        SpringApplication.run(BootUniversity.class, args);
    }
}