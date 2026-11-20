package com.service.webfluxcourse.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class CustomerRouter {

    @Bean
    RouterFunction<ServerResponse> customerEndpoints(CustomerHandler customerHandler) {

        return RouterFunctions.route(POST("/customers").and(accept(MediaType.APPLICATION_JSON)), customerHandler::postCustomer)
                .andRoute(GET("/customers").and(accept(MediaType.APPLICATION_JSON)), customerHandler::getAllCustomers)
                .andRoute(GET("/customers/{customerId}").and(accept(MediaType.APPLICATION_JSON)), customerHandler::getCustomerById)
                .andRoute(PUT("/customers/{customerId}").and(accept(MediaType.APPLICATION_JSON)), customerHandler::putConsumer)
                .andRoute(DELETE("/customers/{customerId}").and(accept(MediaType.APPLICATION_JSON)), customerHandler::deleteConsumer);

    }
}
