package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse book = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::throwClientException)
                    .body(BookApiResponse.class);
            if (book == null) {
                throw new ClientException("External service returned an empty body for book " + id);
            }
            return toDto(book);
        } catch (RestClientException e) {
            throw new ClientException("Failed to fetch book " + id, e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] books = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::throwClientException)
                    .body(BookApiResponse[].class);
            if (books == null) {
                throw new ClientException("External service returned an empty body for all books");
            }
            return Arrays.stream(books)
                    .map(this::toDto)
                    .toList();
        } catch (RestClientException e) {
            throw new ClientException("Failed to fetch all books", e);
        }
    }

    private void throwClientException(HttpRequest request, ClientHttpResponse response) throws IOException {
        throw new ClientException("External service returned " + response.getStatusCode()
                + " for " + request.getMethod() + " " + request.getURI());
    }

    private BookDto toDto(BookApiResponse book) {
        return new BookDto(book.title(), book.author(), book.genre(), book.price());
    }
}
