package com.service.webfluxcourse.controller;

import com.service.webfluxcourse.domain.CustomerModel;
import com.service.webfluxcourse.service.CustomerService;
import com.service.webfluxcourse.validation.CustomerValidator;
import lombok.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Value
@RestController
@RequestMapping(
        value = "/api/v1/customers",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class CustomerRestController {

    CustomerService customerService;
    CustomerValidator customerValidator;

    @GetMapping("/{id}")
    Mono<ResponseEntity<CustomerModel>> getCusomerById(@PathVariable("id") UUID id) {
        return customerService.findCustomerById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Mono<ResponseEntity<CustomerModel>> saveCustomer(@RequestBody CustomerModel customerModel) {
        return customerValidator.validate(customerModel)
                .flatMap( customerService::createCustomer)
                .map(savedCustomer ->
                        ResponseEntity.created(URI.create("/customers/" + savedCustomer.getCustomerId())).body(savedCustomer))
                .onErrorResume(err->Mono.just(ResponseEntity.badRequest().build()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Mono<ResponseEntity<CustomerModel>> updateCustomer(@PathVariable("id") UUID id, @RequestBody CustomerModel customerModel) {

        return customerValidator.validate(customerModel)
                .flatMap(customerModel1 -> customerService.updateCustomer(id, customerModel1)).map(
                ResponseEntity::ok).onErrorResume(err->Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    Flux<CustomerModel> getAllCustomers() {
        return customerService.findAllCustomers();
    }

    @DeleteMapping(value = "/{id}")
    Mono<Void> deleteCustomer(@PathVariable("id") UUID id) {
        return customerService.removeCustomer(id);
    }
}
