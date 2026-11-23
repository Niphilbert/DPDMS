package com.itzetech.dpdms.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_line")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    protected InvoiceLine() {
    }

    public InvoiceLine(UUID invoiceId, int sortOrder, String description, BigDecimal quantity, long unitPrice, long lineTotal) {
        this.invoiceId = invoiceId;
        this.sortOrder = sortOrder;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public long getLineTotal() {
        return lineTotal;
    }
}
