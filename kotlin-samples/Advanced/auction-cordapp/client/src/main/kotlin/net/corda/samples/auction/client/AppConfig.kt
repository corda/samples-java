package net.corda.samples.auction.client

import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.corda.client.jackson.JacksonSupport
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class AppConfig : WebMvcConfigurer{

    @Value("\${partyA.host}")
    lateinit var partyAHostAndPort: String

    @Value("\${partyB.host}")
    lateinit var partyBHostAndPort: String

    @Value("\${partyC.host}")
    lateinit var partyCHostAndPort: String


    @Bean(destroyMethod = "")
    open fun partyAProxy(): CordaRPCOps {
        val partyAClient = CordaRPCClient(NetworkHostAndPort.parse(partyAHostAndPort))
        return partyAClient.start("user1", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun partyBProxy(): CordaRPCOps {
        val partyBClient = CordaRPCClient(NetworkHostAndPort.parse(partyBHostAndPort))
        return partyBClient.start("user1", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun partyCProxy(): CordaRPCOps {
        val partyCClient = CordaRPCClient(NetworkHostAndPort.parse(partyCHostAndPort))
        return partyCClient.start("user1", "test").proxy
    }

    /**
     * Corda Jackson Support, to convert corda objects to json
     */
    @Bean
    open fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(partyAProxy())
        mapper.registerModule(KotlinModule())
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}
