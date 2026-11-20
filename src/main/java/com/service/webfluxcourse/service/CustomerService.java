package com.service.webflux.service;

import com.service.webflux.domain.CustomerModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {

    Mono<CustomerModel> createCustomer(CustomerModel customerModel);

    Mono<CustomerModel> updateCustomer(String id, CustomerModel customerModel);

    Mono<Void> removeCustomer(String customerId);

    Mono<CustomerModel> findCustomerById(String id);

    Flux<CustomerModel> findAllCustomers();
}
