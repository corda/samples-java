package net.corda.samples.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    public interface CONSTANTS {
        String CORDA_USER_NAME = "config.rpc.username";
        String CORDA_USER_PASSWORD = "config.rpc.password";
        String CORDA_NODE_HOST = "config.rpc.host";
        String CORDA_RPC_PORT = "config.rpc.port";
    }

    @Value("${" + CONSTANTS.CORDA_NODE_HOST + "}") String host;
    @Value("${" + CONSTANTS.CORDA_USER_NAME + "}") String username;
    @Value("${" + CONSTANTS.CORDA_USER_PASSWORD + "}") String password;
    @Value("${" + CONSTANTS.CORDA_RPC_PORT + "}") int rpcPort;

    @Bean(destroyMethod = "")  // Avoids node shutdown on rpc disconnect
    public CordaRPCOps rpcProxy(){
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        return rpcClient.start(username, password).getProxy();
    }

    /**
     * Corda Jackson Support, to convert corda objects to json
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
        ObjectMapper mapper =  JacksonSupport.createDefaultMapper(rpcProxy());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        return converter;
    }
}
