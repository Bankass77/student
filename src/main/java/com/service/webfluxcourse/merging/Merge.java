package com.service.webfluxcourse.merging;

import com.service.webfluxcourse.entities.Student;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;

public class Merge {

    public static void main(String[] args) {
        Mono<String> nameMono= Mono.just("John");
        Mono<Double> gpaMono= Mono.just(4.0);
        Mono<Tuple2<String, Double>> zippedMono= nameMono.zipWith(gpaMono);
        Mono<Student> studentMono= zippedMono.map(t-> new Student(t.getT1(), t.getT2()));
        Student result= studentMono.block();
        System.out.println("Message is: " + result.getName());

        System.out.println("Message is concat Flux: " );
        Flux<Integer> num1= Flux.just(1,3,5,7,9);
        Flux<Integer> num2= Flux.just(2,4,6,8,10);
        Flux<Integer> concatResult= Flux.concat(num1.delayElements(Duration.ofSeconds(2)), num2.delayElements(Duration.ofSeconds(1)));

        Iterable<Integer> concatIterable= concatResult.toIterable();
        concatIterable.forEach(System.out::println);

        System.out.println("Message is merge Flux: " );
        Flux<Integer> mergeResult= Flux.merge(num1.delayElements(Duration.ofSeconds(2)), num2.delayElements(Duration.ofSeconds(1)));
        Iterable<Integer> mergeIterable= mergeResult.toIterable();
        mergeIterable.forEach(System.out::println);

    }
}
