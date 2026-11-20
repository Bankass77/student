package com.service.webflux.creation;

import com.service.webflux.entities.Student;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Creation {

    public static void main(String[] args) {

        // Creation Mono instances
        //Optional<Student> getStudentById(UUID id);
        //List<Student> getAllStudents(UUID studentId);

        //Mono<Student> getStudent(UUID id);
        // Flux<Student> getAllStudent(UUID id);


        // Create Mono from plain java Objects:

        // call just() method
        Mono<Student> mono = Mono.just(new Student("Aneta", 3.9));

        Student result = mono.block();// Dans la vie on appelle pas la méthode block()
        System.out.println(result);

/*        Mono<Student> mono1 = Mono.just( null); // NPE
        Student result1= mono1.block();
        System.out.println(result1);*/

        // Create Mono with the justOrEmpty() method
        Mono<Student> mono2 = Mono.justOrEmpty(null); //pour éviter NPE on utilise justOrEmpty
        Student result2 = mono2.block();
        System.out.println(result2);

        // Create Mono with the blockOptional() method
        Mono<Student> mono3 = Mono.justOrEmpty(null);
        Optional<Student> optionalStudent = mono3.blockOptional();
        System.out.println(optionalStudent);

        // Create Mono with the empty() method
        Mono<Student> mono4 = Mono.empty();


        // Create a failed  Mono
        Mono<String> mono5 = Mono.error(new NoSuchFieldException());
        //String mame= mono5.block();  OR
        String mame = mono5.onErrorReturn("Barbara").block();
        // System.out.println(mame);

        // Use Mono as a wrapper for java asynchronous types

        Mono<Student> mono6 = Mono.fromCallable(() -> new Student("Pascal", 6.9));
        System.out.println(mono6.block());

        Mono<Student> mono7 = Mono.fromFuture(CompletableFuture.failedFuture(new NoSuchFieldException()));
        //System.out.println(mono7.block());

        Mono<Student> mono8 = Mono.fromRunnable(() -> new Student("Ibrahim", 9.9));

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .GET()
                .uri(URI.create("https://jsonplaceholder.typicode.com/todos/1"))
                .build();

        CompletableFuture<String> response = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(res -> res.body());

        Mono<String> mono9 =Mono.fromFuture(response);

        String body = mono9.block();
        System.out.println(body);


        // Create Flux Instance:

        // From iterables

        List<Student> students= List.of(new Student("Pascal", 9.9), new Student("Aneta", 3.9),
                new Student("Barbara", 6.9),  new Student("Ibrahim", 9.9),
                new Student("Fatoumata", 7.5));

        Flux<Student> flux = Flux.fromIterable(students);
        System.out.println(flux.collectList().block());

        Student first= flux.blockFirst();
        System.out.println(first);
        Student last= flux.blockLast();
        System.out.println(last);
    }
}
