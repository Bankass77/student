package com.service.webflux.subscription;

import reactor.core.publisher.Mono;

public class Subcriptions {
    public static void main(String[] args) {


        Mono<String> mono = Mono.just("Hello, World!");
        String result = mono
                .doOnTerminate(() -> System.out.println("Mono was terminted!"))
                .doOnError(t -> System.out.println("Error result"))
                .doOnSuccess(s -> System.out.println("Successful result!")).block();

        System.out.println("Message is: " + result);

        Mono<String> mono1 = Mono.error(new RuntimeException("Error!"));
        String result1= mono1
                .doOnTerminate(()->System.out.println("Mono was terminted!"))
                .doOnError(t-> System.out.println("Error result!"))
                .doOnSuccess( s-> System.out.println("Successful result!"))
                .block();
        System.out.println("Message is: " + result1);
    }
}
