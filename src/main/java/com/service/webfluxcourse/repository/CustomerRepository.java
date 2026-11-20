package com.service.webfluxcourse.repository;

import com.service.webfluxcourse.domain.CustomerModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface CustomerRepository extends ReactiveCrudRepository<CustomerModel, UUID> {
}
