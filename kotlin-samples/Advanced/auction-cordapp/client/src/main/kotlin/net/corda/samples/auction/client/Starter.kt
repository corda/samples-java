package net.corda.samples.auction.client

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * Our Spring Boot application.
 */
@EnableAutoConfiguration
@ComponentScan
@SpringBootApplication
open class Starter{
    companion object {

        /**
         * Starts our Spring Boot application.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val app = SpringApplication(Starter::class.java)
            app.setBannerMode(Banner.Mode.OFF)
            app.webApplicationType = SERVLET
            app.run(*args)
        }
    }
}

