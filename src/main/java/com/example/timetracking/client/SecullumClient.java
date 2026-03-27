package com.example.timetracking.client;

import com.example.timetracking.dto.SecullumResponse;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SecullumClient {

    private final WebClient webClient;

    public SecullumClient(WebClient.Builder builder, @Value("${secullum.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<List<SecullumResponse>> fetchEvents(UUID companyId, YearMonth period) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/secullum/events")
                        .queryParam("companyId", companyId)
                        .queryParam("period", period)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(SecullumResponse.class)
                .collectList();
    }
}
