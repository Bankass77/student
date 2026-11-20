package com.service.webfluxcourse.domain;

import com.service.webfluxcourse.entities.Student;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTest {

    @Test
    void create_student_with_mono_java_plain_objects() {

        Mono<Student> mono = Mono.just(new Student("Aneta", 3.9));
        Student result = mono.block();
        assertThat(result.getName()).isEqualTo("Aneta");

        Optional<Student> optionalStudent = mono.blockOptional();
        assertThat(optionalStudent).isNotEmpty();
    }


    @Test
    void create_a_failed_mono() {

        Mono<String> mono = Mono.error(new NoSuchFieldException());
        String name = mono.onErrorReturn("Barbara").block();
        assertThat(name).isEqualTo("Barbara");
    }

    @Test
    void create_flux_with_iterables() {
        List<Student> students = List.of(new Student("Pascal", 9.9), new Student("Aneta", 3.9),
                new Student("Barbara", 6.9), new Student("Ibrahim", 9.9),
                new Student("Fatoumata", 7.5));


        // Create Flux from fromIterable()
        Flux<Student> flux = Flux.fromIterable(students);
        Student first = flux.blockFirst();
        Student last = flux.blockLast();

        assertThat(first.getName()).isEqualTo("Pascal");
        assertThat(last.getName()).isEqualTo("Fatoumata");

        Mono<Student> mono = flux.elementAt(2);
        Student result = mono.block();
        assertThat(result.getName()).isEqualTo("Barbara");

        Iterable<Student> iterable = flux.toIterable();

        assertThat(iterable).hasSameElementsAs(students);

        // Create Flux from Streams

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        Stream<Integer> stream = numbers.stream().filter(i -> i % 2 == 0);
        Flux<Integer> flux2 = Flux.fromStream(stream);

        int firstNumber = flux2.blockFirst();
        assertThat(firstNumber).isEqualTo(2);


        // Ici le stream sera déjà consommé
        //int lastNumber = flux2.blockLast();
        //assertThat(lastNumber).isEqualTo(16);

        // A cause de cette erreur on peut être améner à convertir le Flux en en Iterable:
       // Iterable<Integer> iterable2 = flux2.toIterable();
        //assertThat(iterable2).hasSameElementsAs(numbers);
    }


    // Merge Mono
    @Test
    void merge_mono_zipWith_return_a_tuple(){
        Mono<String> nameMono= Mono.just("John");
        Mono<Double> gpaMono= Mono.just(4.0);
        Mono<Tuple2<String, Double>> zippedMono= nameMono.zipWith(gpaMono);
        Mono<Student> studentMono= zippedMono.map(t-> new Student(t.getT1(), t.getT2()));
        Student result= studentMono.block();

        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getGpa()).isEqualTo(4.0);
    }
}