package com.service.webflux.pipeline;

import com.service.webflux.service.StudentService;
import reactor.core.publisher.Mono;

public class Pipelines {

    public static void main(String[] args) {

        StudentService studentService = new StudentService();

            Mono<Double> result = studentService.getStudentByName("Aneta").
                    flatMap(s -> Mono.just(s.getGpa()))
                    .doOnError(err -> System.out.println("Error occured !"))
                    .onErrorReturn(5.0)
                    .switchIfEmpty(Mono.just(4.0));

            Double value= result.block();
            System.out.println(value);

    }
}
