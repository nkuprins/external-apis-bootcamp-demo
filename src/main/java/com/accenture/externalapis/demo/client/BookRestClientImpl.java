package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// No retries on purpose: in this blocking client every attempt and backoff wait would hold the
// caller's request thread.
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
                    .onStatus(HttpStatusCode::isError, (request, response) ->
                            throwClientException(response, "book " + id))
                    .body(BookApiResponse.class);
            if (book == null) {
                throw new ClientException("External service returned an empty body for book " + id);
            }
            return toDto(book);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unreachable or timed out for book " + id, e);
        } catch (RestClientException e) {
            throw new ClientException("Unreadable response for book " + id, e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] books = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) ->
                            throwClientException(response, "all books"))
                    .body(BookApiResponse[].class);
            if (books == null) {
                throw new ClientException("External service returned an empty body for all books");
            }
            return Arrays.stream(books)
                    .map(this::toDto)
                    .toList();
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unreachable or timed out for all books", e);
        } catch (RestClientException e) {
            throw new ClientException("Unreadable response for all books", e);
        }
    }

    private void throwClientException(ClientHttpResponse response, String target) throws IOException {
        throw new ClientException("External service returned " + response.getStatusCode() + " for " + target);
    }

    private BookDto toDto(BookApiResponse book) {
        if (book.price() == null) {
            throw new ClientException("External service returned no price for book " + book.id());
        }
        return new BookDto(book.title(), book.author(), book.genre(), book.price());
    }
}
