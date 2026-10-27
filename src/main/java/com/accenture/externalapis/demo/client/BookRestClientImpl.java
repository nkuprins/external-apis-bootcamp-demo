package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    // TODO: Handle the main RestClient error cases and rethrow them as ClientException:
    //  - HttpClientErrorException (4xx, e.g. book not found)
    //  - HttpServerErrorException (5xx, e.g. the faulty/teapot book)
    //  - ResourceAccessException (connection refused / timeout - the external service is unreachable)

    @Override
    public BookDto getBook(Long id) {
        BookApiResponse book = restClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .body(BookApiResponse.class);
        return toDto(book);
    }

    @Override
    public List<BookDto> getAllBooks() {
        BookApiResponse[] books = restClient.get()
                .uri("/books")
                .retrieve()
                .body(BookApiResponse[].class);
        return Arrays.stream(books)
                .map(this::toDto)
                .toList();
    }

    private BookDto toDto(BookApiResponse book) {
        return new BookDto(book.title(), book.author(), book.genre(), book.price());
    }
}
