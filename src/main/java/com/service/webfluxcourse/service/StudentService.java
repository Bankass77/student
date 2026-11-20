package com.service.webfluxcourse.service;

import com.service.webfluxcourse.entities.Student;
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
