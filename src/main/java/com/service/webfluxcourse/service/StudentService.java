package com.service.webflux.service;

import com.service.webflux.entities.Student;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StudentService {

    public Mono<Student> getStudentByName(String name) {
       // throw new RuntimeException();
        return Mono.error(new RuntimeException());
    }



    public Flux<Student> findAllStudents() {
        throw  new RuntimeException();

    }
}
