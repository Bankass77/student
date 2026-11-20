package com.service.webfluxcourse.validation;

import reactor.core.publisher.Mono;

public interface BaseValidator<T> {
    Mono<T> validate(T data);
}
