package com.akshara.api.bookimport.provider;

import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookResult;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import com.akshara.api.bookimport.exception.ExternalCatalogueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OpenLibraryBookMetadataProvider implements BookMetadataProvider {

    private static final String RESULT_FIELDS =
            "key,title,subtitle,author_name,publisher,language,subject,"
                    + "first_publish_year,isbn,cover_i,number_of_pages_median,"
                    + "edition_key,publish_date,description,editions";

    private static final int MAX_CACHE_ENTRIES = 200;

    private final RestClient client;
    private final Duration cacheTtl;
    private final Clock clock;
    private final ConcurrentMap<ExternalBookSearchQuery, CachedSearch> cache =
            new ConcurrentHashMap<>();

    @Autowired
    public OpenLibraryBookMetadataProvider(
            @Value("${app.book-metadata.open-library.base-url:https://openlibrary.org}") String baseUrl,
            @Value("${app.book-import.contact:admin@akshara.local}") String contact,
            @Value("${app.book-metadata.open-library.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${app.book-metadata.open-library.read-timeout-ms:25000}") int readTimeout,
            @Value("${app.book-metadata.open-library.cache-ttl-seconds:300}") long cacheTtlSeconds
    ) {
        this(buildClient(baseUrl, contact, connectTimeout, readTimeout),
                Duration.ofSeconds(cacheTtlSeconds), Clock.systemUTC());
    }

    OpenLibraryBookMetadataProvider(RestClient client) {
        this(client, Duration.ofMinutes(5), Clock.systemUTC());
    }

    OpenLibraryBookMetadataProvider(RestClient client, Duration cacheTtl, Clock clock) {
        this.client = client;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    private static RestClient buildClient(
            String baseUrl,
            String contact,
            int connectTimeout,
            int readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "AksharaBookstore/1.0 (" + contact + ")")
                .build();
    }

    @Override
    public BookImportSource source() {
        return BookImportSource.OPEN_LIBRARY;
    }

    @Override
    public ExternalBookSearchPage search(ExternalBookSearchQuery query) {
        CachedSearch cached = cache.get(query);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.page();
        }

        ExternalBookSearchPage result = fetchWithSingleRetry(query);
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        cache.put(query, new CachedSearch(result, now.plus(cacheTtl)));
        return result;
    }

    private ExternalBookSearchPage fetchWithSingleRetry(ExternalBookSearchQuery query) {
        RestClientException firstFailure;
        try {
            return fetch(query);
        } catch (RestClientException exception) {
            firstFailure = exception;
        }
        try {
            return fetch(query);
        } catch (RestClientException exception) {
            exception.addSuppressed(firstFailure);
            throw new ExternalCatalogueException(
                    "Open Library is temporarily unavailable. Please retry or use manual entry.",
                    exception
            );
        }
    }

    private ExternalBookSearchPage fetch(ExternalBookSearchQuery query) {
        int offset = (query.page() - 1) * query.size();
        JsonNode root = client.get()
                .uri(uriBuilder -> uriBuilder.path("/search.json")
                        .queryParam("q", query.query())
                        .queryParam("limit", query.size())
                        .queryParam("offset", offset)
                        .queryParam("fields", RESULT_FIELDS)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        List<ExternalBookResult> results = new ArrayList<>();
        for (JsonNode work : array(root, "docs")) {
            JsonNode editions = work.path("editions").path("docs");
            if (editions.isArray() && !editions.isEmpty()) {
                for (JsonNode edition : editions) results.add(mapResult(work, edition));
            } else {
                results.add(mapResult(work, work));
            }
        }
        long total = root == null ? 0 : root.path("numFound").asLong(0);
        return new ExternalBookSearchPage(
                source(), List.copyOf(results), query.page(), query.size(), total
        );
    }

    private ExternalBookResult mapResult(JsonNode work, JsonNode edition) {
        List<String> isbns = strings(edition, "isbn");
        if (isbns.isEmpty()) isbns = strings(work, "isbn");
        String isbn13 = isbns.stream().map(this::cleanIsbn)
                .filter(value -> value.length() == 13).findFirst().orElse(null);
        String isbn10 = isbns.stream().map(this::cleanIsbn)
                .filter(value -> value.length() == 10).findFirst().orElse(null);
        String coverId = text(edition, "cover_i", text(work, "cover_i", null));
        String cover = coverId == null ? null
                : "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
        String publishDate = firstText(edition, "publish_date");
        if (publishDate == null) publishDate = firstText(work, "publish_date");
        if (publishDate == null && work.has("first_publish_year")) {
            publishDate = work.path("first_publish_year").asText();
        }
        return new ExternalBookResult(
                source(), text(work, "key", null),
                text(edition, "key", firstText(work, "edition_key")),
                text(edition, "title", text(work, "title", "Untitled")),
                text(edition, "subtitle", text(work, "subtitle", null)),
                description(edition, work), preferStrings(edition, work, "author_name"),
                firstPreferredText(edition, work, "publisher"),
                firstPreferredText(edition, work, "language"),
                positiveInt(edition, "number_of_pages",
                        positiveInt(work, "number_of_pages_median", null)),
                limited(strings(work, "subject"), 12), cover,
                normalizeDate(publishDate), isbn10, isbn13,
                text(edition, "edition_name", null), false
        );
    }

    private Iterable<JsonNode> array(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value != null && value.isArray() ? value : List.of();
    }

    private List<String> strings(JsonNode node, String field) {
        JsonNode value = node.path(field);
        Set<String> values = new LinkedHashSet<>();
        if (value.isArray()) value.forEach(item -> {
            if (!item.asText().isBlank()) values.add(item.asText().trim());
        });
        else if (value.isTextual() && !value.asText().isBlank()) {
            values.add(value.asText().trim());
        }
        return List.copyOf(values);
    }

    private List<String> preferStrings(JsonNode primary, JsonNode fallback, String field) {
        List<String> values = strings(primary, field);
        return values.isEmpty() ? strings(fallback, field) : values;
    }

    private String firstPreferredText(JsonNode primary, JsonNode fallback, String field) {
        String value = firstText(primary, field);
        return value == null ? firstText(fallback, field) : value;
    }

    private String firstText(JsonNode node, String field) {
        List<String> values = strings(node, field);
        return values.isEmpty() ? null : values.get(0);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isValueNode() && !value.asText().isBlank()
                ? value.asText().trim() : fallback;
    }

    private Integer positiveInt(JsonNode node, String field, Integer fallback) {
        int value = node.path(field).asInt(0);
        return value > 0 ? Integer.valueOf(value) : fallback;
    }

    private String description(JsonNode primary, JsonNode fallback) {
        JsonNode value = primary.path("description");
        if (value.isMissingNode()) value = fallback.path("description");
        if (value.isObject()) value = value.path("value");
        return value.isTextual() ? value.asText() : null;
    }

    private List<String> limited(List<String> values, int max) {
        return values.size() <= max ? values : values.subList(0, max);
    }

    private String cleanIsbn(String value) {
        return value == null ? "" : value.replaceAll("[-\\s]", "").toUpperCase();
    }

    private String normalizeDate(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) return trimmed;
        if (trimmed.matches("\\d{4}-\\d{2}")) return trimmed + "-01";
        if (trimmed.matches("\\d{4}")) return trimmed + "-01-01";
        return null;
    }

    private record CachedSearch(ExternalBookSearchPage page, Instant expiresAt) { }
}
