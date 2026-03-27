package com.example.timetracking.repository;

import com.example.timetracking.domain.IntegrationConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, UUID> {
    Optional<IntegrationConfig> findByCompanyIdAndActiveTrue(UUID companyId);
}
