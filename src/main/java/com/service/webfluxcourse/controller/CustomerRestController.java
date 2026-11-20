package com.service.webflux.controller;

import com.service.webflux.domain.CustomerModel;
import com.service.webflux.service.CustomerService;
import lombok.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@Value
@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
)
public class CustomerRestController {

    CustomerService customerService;

    @GetMapping("/customer/{id}")
    Mono<ResponseEntity<CustomerModel>> getCusomerById(@PathVariable("id") String id) {
        return customerService.findCustomerById(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    Mono<ResponseEntity<CustomerModel>> saveCustomer(@RequestBody CustomerModel customerModel) {
        return customerService.createCustomer(customerModel)
                .map(savedCustomer ->
                        ResponseEntity.created(URI.create("/customers/" + savedCustomer.getCustomerId())).body(savedCustomer));
    }

    @PutMapping("/customer/{id}")
    Mono<ResponseEntity<CustomerModel>> updateCustomer(@PathVariable("id") String id, @RequestBody CustomerModel customerModel) {

        return customerService.updateCustomer(id, customerModel).map(
                ResponseEntity::ok);
    }

    @GetMapping("/customers")
    Flux<CustomerModel> getAllCustomers() {
        return customerService.findAllCustomers();
    }

    @DeleteMapping("/customer/{id}")
    Mono<Void> deleteCustomer(@PathVariable("id") String id) {
        return customerService.removeCustomer(id);
    }
}
