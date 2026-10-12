package vn.comtruatrachieu.be.bank;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final RestClient vietQrClient;
    private final String clientId;
    private final String apiKey;

    public QrController(RestClient vietQrClient,
                        @Value("${vietqr.client-id}") String clientId,
                        @Value("${vietqr.api-key}") String apiKey) {
        this.vietQrClient = vietQrClient;
        this.clientId = clientId;
        this.apiKey = apiKey;
    }

    /** Proxy to POST https://api.vietqr.io/v2/generate (credentials stay on the server). */
    @PostMapping
    public Map<String, Object> generate(@RequestBody Map<String, Object> request) {
        return vietQrClient.post()
                .uri("/v2/generate")
                .header("x-client-id", clientId)
                .header("x-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
