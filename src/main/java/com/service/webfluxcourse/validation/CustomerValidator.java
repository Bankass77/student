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
                // Format TVA français: FR : préfixe du pays,2 caractères de clé de contrôle,9 chiffres correspondant au numéro SIREN. exemple:FR40303265079
                .constraint(CustomerModel::getTaxId, "taxId", c -> c.notBlank().pattern("^FR[0-9A-HJ-NP-Z]{2}[0-9]{9}$")).build();
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
