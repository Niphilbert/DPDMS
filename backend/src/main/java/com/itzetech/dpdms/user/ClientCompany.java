package com.itzetech.dpdms.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_company")
public class ClientCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(length = 40)
    private String phone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ClientCompany() {
    }

    public ClientCompany(String name, String contactName, String phone) {
        this.name = name;
        this.contactName = contactName;
        this.phone = phone;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
