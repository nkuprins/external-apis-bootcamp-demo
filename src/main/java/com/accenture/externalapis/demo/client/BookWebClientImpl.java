package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private final WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> toClientException(response, "book " + id))
                .bodyToMono(BookApiResponse.class)
                .switchIfEmpty(Mono.error(() ->
                        new ClientException("External service returned an empty body for book " + id)))
                .map(this::toDto)
                .onErrorMap(e -> mapError(e, "book " + id));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> toClientException(response, "all books"))
                .bodyToFlux(BookApiResponse.class)
                .map(this::toDto)
                .onErrorMap(e -> mapError(e, "all books"));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        return Mono.zip(getBookAsync(id1), getBookAsync(id2), List::of);
    }

    private Throwable mapError(Throwable e, String target) {
        if (e instanceof WebClientRequestException) {
            return new ClientException("External service unreachable or timed out for " + target, e);
        }
        if (e instanceof CodecException || e instanceof WebClientResponseException) {
            return new ClientException("Unreadable response for " + target, e);
        }
        return e;
    }

    private Mono<ClientException> toClientException(ClientResponse response, String target) {
        return Mono.just(new ClientException("External service returned " + response.statusCode() + " for " + target));
    }

    private BookDto toDto(BookApiResponse book) {
        if (book.price() == null) {
            throw new ClientException("External service returned no price for book " + book.id());
        }
        return new BookDto(book.title(), book.author(), book.genre(), book.price());
    }
}
