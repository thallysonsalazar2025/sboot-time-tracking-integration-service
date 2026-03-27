package com.example.timetracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "integration_config")
public class IntegrationConfig {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String config;

    @Column(nullable = false)
    private boolean active;

    public IntegrationConfig() {
    }

    public IntegrationConfig(UUID id, UUID companyId, ProviderType providerType, String config, boolean active) {
        this.id = id;
        this.companyId = companyId;
        this.providerType = providerType;
        this.config = config;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
