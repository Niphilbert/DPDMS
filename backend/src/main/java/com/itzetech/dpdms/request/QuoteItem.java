package com.itzetech.dpdms.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "quote_item")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ClientRequest request;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private long amount;

    protected QuoteItem() {
    }

    public QuoteItem(int sortOrder, String description, long amount) {
        this.sortOrder = sortOrder;
        this.description = description;
        this.amount = amount;
    }

    void attach(ClientRequest request) {
        this.request = request;
    }

    public UUID getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }
}
