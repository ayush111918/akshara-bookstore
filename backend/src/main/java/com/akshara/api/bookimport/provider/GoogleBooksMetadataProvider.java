package com.akshara.api.bookimport.provider;

import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import com.akshara.api.bookimport.exception.ExternalCatalogueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${app.book-metadata.google-books.api-key:}')")
public class GoogleBooksMetadataProvider implements BookMetadataProvider {

    private final RestClient client;
    private final String apiKey;

    @Autowired
    public GoogleBooksMetadataProvider(
            @Value("${app.book-metadata.google-books.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${app.book-metadata.google-books.api-key}") String apiKey,
            @Value("${app.book-metadata.google-books.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${app.book-metadata.google-books.read-timeout-ms:15000}") int readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
    }

    GoogleBooksMetadataProvider(RestClient client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public BookImportSource source() {
        return BookImportSource.GOOGLE_BOOKS;
    }

    @Override
    public ExternalBookSearchPage search(ExternalBookSearchQuery query) {
        try {
            int startIndex = (query.page() - 1) * query.size();
            JsonNode root = client.get().uri(builder -> builder.path("/volumes")
                            .queryParam("q", query.query())
                            .queryParam("startIndex", startIndex)
                            .queryParam("maxResults", query.size())
                            .queryParam("printType", "books")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve().body(JsonNode.class);
            List<ExternalBookResult> results = new ArrayList<>();
            JsonNode items = root == null ? null : root.path("items");
            if (items != null && items.isArray()) items.forEach(item -> results.add(map(item)));
            long total = root == null ? 0 : root.path("totalItems").asLong(0);
            return new ExternalBookSearchPage(source(), List.copyOf(results), query.page(), query.size(), total);
        } catch (RestClientException exception) {
            throw new ExternalCatalogueException(
                    "Google Books is temporarily unavailable. Please retry or use Open Library.",
                    exception
            );
        }
    }

    private ExternalBookResult map(JsonNode item) {
        JsonNode info = item.path("volumeInfo");
        String isbn10 = identifier(info, "ISBN_10");
        String isbn13 = identifier(info, "ISBN_13");
        String thumbnail = text(info.path("imageLinks"), "thumbnail");
        if (thumbnail != null && thumbnail.startsWith("http://")) {
            thumbnail = "https://" + thumbnail.substring(7);
        }
        String id = text(item, "id");
        return new ExternalBookResult(
                source(), id, id, text(info, "title"), text(info, "subtitle"),
                text(info, "description"), strings(info, "authors"), text(info, "publisher"),
                text(info, "language"), positiveInt(info, "pageCount"), strings(info, "categories"),
                thumbnail, normalizeDate(text(info, "publishedDate")), isbn10, isbn13,
                null, false
        );
    }

    private String identifier(JsonNode info, String type) {
        JsonNode identifiers = info.path("industryIdentifiers");
        if (!identifiers.isArray()) return null;
        for (JsonNode identifier : identifiers) {
            if (type.equals(identifier.path("type").asText())) {
                return identifier.path("identifier").asText().replaceAll("[-\\s]", "");
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private List<String> strings(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        value.forEach(item -> { if (!item.asText().isBlank()) result.add(item.asText().trim()); });
        return List.copyOf(result);
    }

    private Integer positiveInt(JsonNode node, String field) {
        int value = node.path(field).asInt(0);
        return value > 0 ? value : null;
    }

    private String normalizeDate(String value) {
        if (value == null) return null;
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) return value;
        if (value.matches("\\d{4}-\\d{2}")) return value + "-01";
        if (value.matches("\\d{4}")) return value + "-01-01";
        return null;
    }
}
