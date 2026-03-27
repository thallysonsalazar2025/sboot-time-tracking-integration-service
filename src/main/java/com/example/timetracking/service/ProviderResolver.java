package com.example.timetracking.service;

import com.example.timetracking.adapter.SecullumAdapter;
import com.example.timetracking.domain.ProviderType;
import org.springframework.stereotype.Component;

@Component
public class ProviderResolver {

    private final SecullumAdapter secullumAdapter;

    public ProviderResolver(SecullumAdapter secullumAdapter) {
        this.secullumAdapter = secullumAdapter;
    }

    public TimeTrackingProvider resolve(ProviderType providerType) {
        if (ProviderType.SECULLUM == providerType) {
            return secullumAdapter;
        }
        throw new IllegalArgumentException("Unsupported provider type: " + providerType);
    }
}
