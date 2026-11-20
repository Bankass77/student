package com.service.webfluxcourse.service;

import com.service.webfluxcourse.domain.CustomerModel;
import com.service.webfluxcourse.repository.CustomerRepository;
import lombok.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Value
public class CustomerServiceImpl implements CustomerService {

    CustomerRepository customerRepository;

    @Override
    public Mono<CustomerModel> createCustomer(CustomerModel customerModel) {
        return customerRepository.save(customerModel);
    }

    @Override
    public Mono<CustomerModel> updateCustomer(UUID id, CustomerModel customerModel) {
        return customerRepository.findById(id)
                .map(existingCustomer ->
                        new CustomerModel(existingCustomer.getCustomerId(),
                                customerModel.getCompanyName(),
                                customerModel.getCompanyEmail(),
                                customerModel.getTaxId(),
                                customerModel.getBillingAddress(), customerModel.getShippingAddress()))
                .flatMap(customerRepository::save);
    }

    @Override
    public Mono<Void> removeCustomer(UUID customerId) {
        return customerRepository.deleteById(customerId);
    }

    @Override
    public Mono<CustomerModel> findCustomerById(UUID id) {
        return customerRepository.findById(id);
    }

    @Override
    public Flux<CustomerModel> findAllCustomers() {
        return customerRepository.findAll();
    }
}
