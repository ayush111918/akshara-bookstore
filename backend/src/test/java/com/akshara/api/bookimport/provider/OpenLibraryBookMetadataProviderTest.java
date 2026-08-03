package com.akshara.api.bookimport.provider;

import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OpenLibraryBookMetadataProviderTest {

    @Test
    void normalizesEditionMetadataAndEncodedQuery() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://openlibrary.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("q", "Atomic%20Habits%20%26%20change"))
                .andExpect(queryParam("limit", "10"))
                .andExpect(queryParam("offset", "10"))
                .andRespond(withSuccess("""
                        {"numFound":21,"docs":[{
                          "key":"/works/OL1W","title":"Atomic Habits",
                          "description":"A practical guide to building better habits.",
                          "author_name":["James Clear"],"subject":["Habits"],
                          "editions":{"docs":[{
                            "key":"/books/OL1M","title":"Atomic Habits",
                            "publisher":["Avery"],"publish_date":["2018"],
                            "language":["eng"],"number_of_pages":320,
                            "cover_i":123,"isbn":["0735211299","9780735211292"]
                          }]}
                        }]}
                        """, MediaType.APPLICATION_JSON));

        ExternalBookSearchPage result =
                new OpenLibraryBookMetadataProvider(builder.build()).search(
                        new ExternalBookSearchQuery(
                                "Atomic Habits & change", 2, 10
                        )
                );

        assertEquals(21, result.totalResults());
        assertEquals(2, result.page());
        assertEquals(1, result.results().size());
        assertEquals("/works/OL1W", result.results().get(0).sourceId());
        assertEquals("/books/OL1M", result.results().get(0).editionId());
        assertEquals("9780735211292", result.results().get(0).isbn13());
        assertEquals("2018-01-01", result.results().get(0).publicationDate());
        assertEquals("A practical guide to building better habits.", result.results().get(0).description());
        server.verify();
    }

    @Test
    void missingOptionalFieldsProduceSafeNullsAndLists() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://openlibrary.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { })
                .andRespond(withSuccess(
                        "{\"numFound\":1,\"docs\":[{\"key\":\"/works/X\",\"title\":\"Rare Book\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        ExternalBookSearchPage result =
                new OpenLibraryBookMetadataProvider(builder.build()).search(
                        new ExternalBookSearchQuery("Rare Book", 1, 10)
                );

        assertEquals("Rare Book", result.results().get(0).title());
        assertEquals(0, result.results().get(0).authors().size());
        assertNull(result.results().get(0).coverImageUrl());
        assertNull(result.results().get(0).isbn13());
    }

    @Test
    void zeroMatchesReturnSuccessfulEmptyPage() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://openlibrary.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withSuccess(
                "{\"numFound\":0,\"docs\":[]}", MediaType.APPLICATION_JSON
        ));

        ExternalBookSearchPage result =
                new OpenLibraryBookMetadataProvider(builder.build()).search(
                        new ExternalBookSearchQuery("nothing", 1, 10)
                );

        assertEquals(0, result.totalResults());
        assertEquals(0, result.results().size());
    }

    @Test
    void retriesOneTransientFailureThenCachesTheSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(request -> { }).andRespond(withSuccess(
                "{\"numFound\":1,\"docs\":[{\"key\":\"/works/R\",\"title\":\"Recovered\"}]}",
                MediaType.APPLICATION_JSON
        ));
        OpenLibraryBookMetadataProvider provider =
                new OpenLibraryBookMetadataProvider(builder.build());
        ExternalBookSearchQuery query = new ExternalBookSearchQuery("Recovered", 1, 10);

        assertEquals("Recovered", provider.search(query).results().get(0).title());
        assertEquals("Recovered", provider.search(query).results().get(0).title());
        server.verify();
    }
}
