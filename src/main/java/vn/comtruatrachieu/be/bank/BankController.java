package vn.comtruatrachieu.be.bank;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final RestClient vietQrClient;

    public BankController(RestClient vietQrClient) {
        this.vietQrClient = vietQrClient;
    }

    /** Proxy to GET https://api.vietqr.io/v2/banks */
    @GetMapping
    public Map<String, Object> getBanks() {
        return vietQrClient.get().uri("/v2/banks").retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }
}
