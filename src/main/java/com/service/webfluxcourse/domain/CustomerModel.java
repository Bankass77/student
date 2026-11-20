package com.service.webfluxcourse.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Value
@Table("customers")
public class CustomerModel {
    @Id
    UUID customerId;
    String companyName;
    String companyEmail;
    String taxId;

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "billing_")
    AddressModel billingAddress;
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "shipping_")
    AddressModel shippingAddress;

    @JsonCreator
    public CustomerModel(@JsonProperty("customerId") UUID customerId,
                         @JsonProperty("companyName") String companyName,
                         @JsonProperty("companyEmail") String companyEmail,
                         @JsonProperty("taxId") String taxId,
                         @JsonProperty("billingAddress") AddressModel billingAddress,
                         @JsonProperty("shippingAddress") AddressModel shippingAddress) {
        this.customerId = customerId;
        this.companyName = companyName;
        this.companyEmail = companyEmail;
        this.taxId = taxId;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
    }
}
