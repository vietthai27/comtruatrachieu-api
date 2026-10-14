package vn.comtruatrachieu.be;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${vietqr.base-url}")
    private String vietQrBaseUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // allowedOriginPatterns chấp nhận cả "*" lẫn mẫu như "http://192.168.*:*"
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @Bean
    RestClient vietQrClient() {
        return RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                .baseUrl(vietQrBaseUrl)
                .build();
    }
}
