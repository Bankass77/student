package com.service.webfluxcourse.domain;

import com.service.webfluxcourse.entities.PostModel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class PostModelTest {
    @Test
    void getOnePostRequestTest() {

        WebClient webClient = WebClient.create();

        Mono<PostModel> result = webClient.get()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PostModel.class);
        PostModel postModel = result.block();
        assertThat(postModel).hasFieldOrPropertyWithValue("userId", 1);
    }

    @Test
    void getRequestDataTest() {

        WebClient webClient = WebClient.create();
        Mono<ResponseEntity<PostModel>> result = webClient.get()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(PostModel.class);
        ResponseEntity<PostModel> response = result.block();
        assert response != null;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(response.hasBody()).isTrue();
    }

    @Test
    void getPostTest() {
        WebClient webClient = WebClient.create();
        Flux<PostModel> result = webClient.get()
                .uri(URI.create("http://jsonplaceholder.typicode.com/posts"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(PostModel.class);

        Iterable<PostModel> posts = result.toIterable();

        assertThat(posts).hasSize(100);
    }

    @Test
    void postRequestTest() {
        WebClient webClient = WebClient.create();

        PostModel payload = new PostModel(101, 101, "title", "body");

        Mono<PostModel> result = webClient.post()
                .uri(URI.create("http://jsonplaceholder.typicode.com/posts"))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(PostModel.class);

        PostModel postModel = result.block();
        assertThat(postModel).isEqualTo(payload);
    }
}
