package com.service.webfluxcourse.service;

import com.service.webfluxcourse.domain.CustomerModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerService {

    Mono<CustomerModel> createCustomer(CustomerModel customerModel);

    Mono<CustomerModel> updateCustomer(UUID id, CustomerModel customerModel);

    Mono<Void> removeCustomer(UUID customerId);

    Mono<CustomerModel> findCustomerById(UUID id);

    Flux<CustomerModel> findAllCustomers();
}
