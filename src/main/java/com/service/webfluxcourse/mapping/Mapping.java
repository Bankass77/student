package com.service.webfluxcourse.mapping;

import com.service.webfluxcourse.entities.Student;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Mapping {

    public static void main(String[] args) {

        List<Student> students = List.of(new Student("Aneta", 3.8),
                new Student("Barbara", 3.5), new Student("Carolina", 3.4),
                new Student("Denisa", 2.9),
                new Student("Eva", 2.9));

        Double average = students.stream().mapToDouble(Student::getGpa).average().orElse(0.0);
        System.out.println(average);

        Double average2 = students.stream().collect(Collectors.averagingDouble(Student::getGpa));
        System.out.println(average2);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://jsonplaceholder.typicode.com/todos/1"))
                .build();
        CompletableFuture<HttpResponse<String>> responseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        Mono<HttpResponse<String>> responseMono = Mono.fromFuture(responseCompletableFuture);
        Mono<String> mapped = responseMono.map(HttpResponse::body);
        String body = mapped.block();
        System.out.println(body);

        // map
        Mono<Student> studentMono = Mono.just(new Student("Marketa", 3.5));
        Mono<String> nameMaped = studentMono.map(Student::getName);
        System.out.println("Name mapped: " + nameMaped.block());
        // flatMap
        Mono<String> nameFlatMap = studentMono.flatMap(student -> Mono.just(student.getName()));
        System.out.println("Name flat mapped: " + nameFlatMap.block());

    }

}
