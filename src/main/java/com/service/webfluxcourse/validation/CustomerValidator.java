package com.service.webfluxcourse.validation;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import com.service.webfluxcourse.domain.CustomerModel;

import com.service.webfluxcourse.exception.CustomerValidationException;
import reactor.core.publisher.Mono;

public class CustomerValidator implements BaseValidator<CustomerModel> {
    private final Validator<CustomerModel> validator;

    public CustomerValidator() {
        validator = ValidatorBuilder.of(CustomerModel.class)
                .constraint(CustomerModel::getCompanyName, "companyName", c -> c.notNull())
                .constraint(CustomerModel::getCompanyEmail, "companyEmail", c -> c.notNull().email())
                .constraint(CustomerModel::getTaxId, "taxId", c -> c.notNull().pattern(" ")).build();
    }

    @Override
    public Mono<CustomerModel> validate(CustomerModel data) {

        ConstraintViolations violations = validator.validate(data);

        if (violations.isValid()) {
            return Mono.just(data);
        }
        return Mono.error(new CustomerValidationException(violations.violations()));
    }
}
