package com.itzetech.dpdms.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientCompanyRepository extends JpaRepository<ClientCompany, UUID> {
}
