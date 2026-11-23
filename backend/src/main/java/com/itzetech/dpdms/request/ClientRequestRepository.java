package com.itzetech.dpdms.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientRequestRepository extends JpaRepository<ClientRequest, UUID> {

    List<ClientRequest> findAllByOrderByCreatedAtDesc();

    List<ClientRequest> findByStatusOrderByCreatedAtDesc(ClientRequest.Status status);

    List<ClientRequest> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<ClientRequest> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, ClientRequest.Status status);
}
