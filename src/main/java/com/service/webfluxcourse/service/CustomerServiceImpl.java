package com.service.webflux.service;

import com.service.webflux.domain.CustomerModel;
import com.service.webflux.repository.CustomerRepository;
import lombok.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Value
public class CustomerServiceImpl implements CustomerService {

    CustomerRepository customerRepository;

    @Override
    public Mono<CustomerModel> createCustomer(CustomerModel customerModel) {
        return customerRepository.save(customerModel);
    }

    @Override
    public Mono<CustomerModel> updateCustomer(String id, CustomerModel customerModel) {
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
    public Mono<Void> removeCustomer(String customerId) {
        return customerRepository.deleteById(customerId);
    }

    @Override
    public Mono<CustomerModel> findCustomerById(String id) {
        return customerRepository.findById(id);
    }

    @Override
    public Flux<CustomerModel> findAllCustomers() {
        return customerRepository.findAll();
    }
}
