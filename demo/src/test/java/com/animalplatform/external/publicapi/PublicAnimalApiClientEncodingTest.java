package com.animalplatform.external.publicapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PublicAnimalApiClientEncodingTest {

    @Test
    void fetchSheltersByRegionNameEncodesKoreanQueryAndParsesUtf8Response() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicAnimalApiClient client = new PublicAnimalApiClient(
                new PublicApiConfig().restClient(builder),
                publicApiProperties()
        );

        server.expect(request -> {
                    String rawQuery = request.getURI().getRawQuery();

                    assertThat(rawQuery).contains("regionName=%EC%84%9C%EC%9A%B8%ED%8A%B9%EB%B3%84%EC%8B%9C");
                    assertThat(request.getHeaders().getAcceptCharset()).contains(StandardCharsets.UTF_8);
                })
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "body": {
                              "items": {
                                "item": [
                                  {
                                    "careNm": "서울특별시 동물보호센터",
                                    "orgNm": "서울특별시"
                                  }
                                ]
                              },
                              "totalCount": 1
                            }
                          }
                        }
                        """, new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)));

        List<Map<String, Object>> shelters = client.fetchSheltersByRegionName("서울특별시", 1, 10);

        assertThat(shelters).hasSize(1);
        assertThat(shelters.getFirst())
                .containsEntry("careNm", "서울특별시 동물보호센터")
                .containsEntry("orgNm", "서울특별시");
        server.verify();
    }

    private PublicApiProperties publicApiProperties() {
        PublicApiProperties properties = new PublicApiProperties();
        properties.setProvider("data.go.kr");
        properties.setServiceKey("test-service-key");
        properties.setAbandonmentBaseUrl("https://api.example.test");
        properties.setShelterBaseUrl("https://api.example.test");
        properties.setStatsBaseUrl("https://api.example.test");
        return properties;
    }
}
