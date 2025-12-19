package com.finedine.orderservice.client;

import com.finedine.orderservice.config.RestaurantServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration
@ImportHttpServices(RestaurantServiceClient.class)
public class HttpServicesConfig {

    @Bean
    public RestaurantServiceClient restaurantServiceClient(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder
                .baseUrl("http://localhost:8085")
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(RestaurantServiceClient.class);
    }
}