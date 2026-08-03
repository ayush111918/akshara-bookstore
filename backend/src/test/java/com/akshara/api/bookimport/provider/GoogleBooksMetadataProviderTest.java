package com.akshara.api.bookimport.provider;

import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleBooksMetadataProviderTest {

    @Test
    void normalizesVolumeMetadataAndUsesTheConfiguredKey() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://google-books.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("q", "Atomic%20Habits"))
                .andExpect(queryParam("key", "test-key"))
                .andRespond(withSuccess("""
                        {"totalItems":1,"items":[{"id":"volume-1","volumeInfo":{
                          "title":"Atomic Habits","subtitle":"Tiny Changes",
                          "description":"A habits guide","authors":["James Clear"],
                          "publisher":"Avery","publishedDate":"2018","pageCount":320,
                          "categories":["Self-Help"],"language":"en",
                          "imageLinks":{"thumbnail":"http://images.test/cover.jpg"},
                          "industryIdentifiers":[
                            {"type":"ISBN_10","identifier":"0735211299"},
                            {"type":"ISBN_13","identifier":"9780735211292"}
                          ]
                        }}]}
                        """, MediaType.APPLICATION_JSON));

        var result = new GoogleBooksMetadataProvider(builder.build(), "test-key")
                .search(new ExternalBookSearchQuery("Atomic Habits", 1, 10));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).description()).isEqualTo("A habits guide");
        assertThat(result.results().get(0).coverImageUrl()).startsWith("https://");
        assertThat(result.results().get(0).isbn13()).isEqualTo("9780735211292");
        assertThat(result.results().get(0).publicationDate()).isEqualTo("2018-01-01");
        server.verify();
    }
}
