package com.example.timetracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.timetracking.adapter.SecullumAdapter;
import com.example.timetracking.domain.ProviderType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProviderResolverTest {

    @Test
    void shouldResolveSecullumProvider() {
        SecullumAdapter secullumAdapter = Mockito.mock(SecullumAdapter.class);
        ProviderResolver resolver = new ProviderResolver(secullumAdapter);

        assertThat(resolver.resolve(ProviderType.SECULLUM)).isSameAs(secullumAdapter);
    }

    @Test
    void shouldRejectUnsupportedProvider() {
        ProviderResolver resolver = new ProviderResolver(Mockito.mock(SecullumAdapter.class));

        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported provider type");
    }
}
