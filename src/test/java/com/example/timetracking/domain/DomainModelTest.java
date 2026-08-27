package com.example.timetracking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void shouldExposeIntegrationConfigurationState() {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        IntegrationConfig config = new IntegrationConfig(id, companyId, ProviderType.SECULLUM, "{\"key\":\"value\"}", true);

        assertThat(config.getId()).isEqualTo(id);
        assertThat(config.getCompanyId()).isEqualTo(companyId);
        assertThat(config.getProviderType()).isEqualTo(ProviderType.SECULLUM);
        assertThat(config.getConfig()).contains("key");
        assertThat(config.isActive()).isTrue();

        UUID newId = UUID.randomUUID();
        UUID newCompany = UUID.randomUUID();
        config.setId(newId);
        config.setCompanyId(newCompany);
        config.setProviderType(ProviderType.SECULLUM);
        config.setConfig("{}");
        config.setActive(false);

        assertThat(config.getId()).isEqualTo(newId);
        assertThat(config.getCompanyId()).isEqualTo(newCompany);
        assertThat(config.getConfig()).isEqualTo("{}");
        assertThat(config.isActive()).isFalse();
    }

    @Test
    void shouldExposeTimeEventState() {
        LocalDate date = LocalDate.of(2026, 8, 27);
        TimeEvent event = new TimeEvent("OVERTIME", date, BigDecimal.TEN, BigDecimal.ONE);

        assertThat(event.getType()).isEqualTo("OVERTIME");
        assertThat(event.getDate()).isEqualTo(date);
        assertThat(event.getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(event.getAmount()).isEqualByComparingTo(BigDecimal.ONE);

        event.setType("ABSENCE");
        event.setDate(date.plusDays(1));
        event.setQuantity(BigDecimal.ZERO);
        event.setAmount(BigDecimal.ZERO);

        assertThat(event.getType()).isEqualTo("ABSENCE");
        assertThat(event.getDate()).isEqualTo(date.plusDays(1));
        assertThat(event.getQuantity()).isZero();
        assertThat(event.getAmount()).isZero();
    }
}
