package com.service.webfluxcourse.controller;

import com.service.webfluxcourse.domain.CustomerModel;
import com.service.webfluxcourse.service.CustomerService;
import lombok.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Value
@Component
public class CustomerHandler {

    CustomerService customerService;

    Mono<ServerResponse> postCustomer(ServerRequest request) {

        Mono<CustomerModel> body = request.bodyToMono(CustomerModel.class);
        Mono<CustomerModel> result = body.flatMap(customerService::createCustomer);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(result, CustomerModel.class);
    }

    Mono<ServerResponse> putConsumer(ServerRequest request) {

        return request.bodyToMono(CustomerModel.class).
                flatMap(b -> customerService.updateCustomer(b.getCustomerId(), b))
                .flatMap(customerModel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(customerModel));
    }

    Mono<ServerResponse> deleteConsumer(ServerRequest request) {

        UUID id = UUID.fromString(request.pathVariable("customerId"));
        Mono<Void> result = customerService.removeCustomer(id);
        return ServerResponse.ok().body(result, Void.class);
    }


    Mono<ServerResponse> getCustomerById(ServerRequest request) {

        UUID id = UUID.fromString(request.pathVariable("customerId"));
        Mono<CustomerModel> result = customerService.findCustomerById(id);
        return ServerResponse.ok().body(result, CustomerModel.class)
                .switchIfEmpty(ServerResponse.notFound().build());

    }

    Mono<ServerResponse> getAllCustomers(ServerRequest request) {

        Flux<CustomerModel> result = customerService.findAllCustomers();
        return ServerResponse.ok().body(result, CustomerModel.class);
    }
}
