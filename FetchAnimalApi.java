import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FetchAnimalApi {
  public static void main(String[] args) throws Exception {
    String serviceKey = System.getenv("PUBLIC_API_SERVICE_KEY");
    if (serviceKey == null || serviceKey.isBlank()) {
      throw new IllegalStateException("PUBLIC_API_SERVICE_KEY is required.");
    }

    String url = "https://apis.data.go.kr/1543061/abandonmentPublicService_v2/abandonmentPublic_v2"
        + "?serviceKey=" + java.net.URLEncoder.encode(serviceKey, java.nio.charset.StandardCharsets.UTF_8)
        + "&pageNo=1&numOfRows=1&_type=json";
    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .header("Accept", "application/json")
        .header("User-Agent", "Mozilla/5.0")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
