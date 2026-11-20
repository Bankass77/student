package com.service.webfluxcourse.webClient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class CreateWebClientInstance {

    public static void main(String[] args) {

        //WebClient with create and no parameters
        WebClient baseClient = WebClient.create();

        //WebClient with baseUrl as parameter
        WebClient baseUrl = WebClient.create("https://myserver.com");


        //WebClient with builder
        WebClient builderClient = WebClient.builder()
                .baseUrl("https://myserver.com")
                .defaultCookie("key", "value")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
