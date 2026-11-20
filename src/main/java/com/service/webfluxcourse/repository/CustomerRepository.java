package com.service.webflux.repository;

import com.service.webflux.domain.CustomerModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CustomerRepository extends ReactiveCrudRepository<CustomerModel, String> {
}
